package omsu.imit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import omsu.imit.interfaces.HttpRequest;
import omsu.imit.interfaces.IInn;
import omsu.imit.models.Inn;
import omsu.imit.models.Kkt;
import omsu.imit.repo.KktCrudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


@Service
public class KktService {

    Gson gson = new Gson();

    private final AtomicInteger attemptsCount = new AtomicInteger(1);

    @Autowired
    private KktCrudRepository kktCrudRepository;

    @Autowired
    private HttpRequest httpRequest;

    @Autowired
    private IInn iInn;

    /**
     * Следующие два булева необходимы, чтобы
     * Правильно отображать в UI состояние
     * Базы: Обновлена, обновляется, произошла ошибка при обновлении
     */
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    private final AtomicBoolean isErr = new AtomicBoolean(false);


    private final Logger LOGGER = LoggerFactory.getLogger(KktService.class);

    public List<Kkt> getAllKktByInn(long inn) {
        return kktCrudRepository.getKkts(Objects.requireNonNull(iInn.getInfoAboutCertainInn(inn).getBody()).getId());
    }

    public Kkt getKktById(long id) {
        return kktCrudRepository.findById(id);
    }

    public void deleteAllKktByInn(long inn) {
        kktCrudRepository.deleteKktByInn(inn);
    }

    public void updateLastTimeUpdated(List<Kkt> kktList){
        kktList.forEach(s->kktCrudRepository.updateLastTimeUpdated(LocalDateTime.now().toString(),Long.parseLong(s.getKktRegNumber())));
    }


    public ResponseEntity<?> insertOrUpdateKktFromInn(long inn, boolean update, String token) {
        try {
            isUpdating.getAndSet(true);
            ResponseEntity<String> responseEntity = httpRequest.getPostsPlainJSON
                    ("https://ofd.ru/api/integration/v1/inn/" + inn + "/kkts?AuthToken=+" + token);
            Inn main = iInn.getInfoAboutCertainInn(inn).getBody();
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                JsonArray kkts = gson.fromJson(responseEntity.getBody(), JsonObject.class).getAsJsonArray("Data");
                if (kkts.size() > 0) {
                    List<Kkt> list = new ArrayList<>();
                    kkts.forEach(s -> {
                        JsonObject obj = gson.fromJson(s, JsonObject.class);
                        list.add(new Kkt(
                                obj.get("FnNumber").getAsString(),
                                obj.get("FnEndDate").getAsString(),
                                obj.get("SerialNumber").getAsString(),
                                obj.get("KktRegId").getAsString(),
                                LocalDateTime.parse(obj.get("FirstDocumentDate").getAsString()),
                                LocalDateTime.parse(obj.get("LastDocOnKktDateTime").getAsString()),
                                null,
                                obj.get("FiscalAddress").getAsString(),
                                obj.get("FiscalPlace").getAsString(),
                                obj.get("KktModel").getAsString(),
                                main
                        ));
                    });
                    if (!update) {
                        kktCrudRepository.saveAll(list);
                    } else {
                        list.forEach(s -> {
                                    if (kktCrudRepository.findByKktRegNumber(s.getKktRegNumber()) != null) {
                                        kktCrudRepository.updateKkt(s.getFnNumber(), s.getFnEndDate(),
                                                s.getFiscalAddress(), s.getFiscalPlace(),
                                                s.getLastDocOnOfdDateTime().toString(),
                                                Long.parseLong(s.getKktRegNumber()));
                                    } else {
                                        kktCrudRepository.save(s);
                                    }
                                }
                        );
                    }
                    isUpdating.getAndSet(false);
                    isErr.getAndSet(false);
                    LOGGER.info("ККТ были успешно обновлены в базе, очередь чеков.");
                    attemptsCount.set(1);
                    return new ResponseEntity<>("ККТ были успешно обновлены в базе, очередь чеков.",
                            HttpStatus.OK);
                } else {
                    isUpdating.getAndSet(false);
                    isErr.getAndSet(false);
                    attemptsCount.set(1);
                    LOGGER.error("KKTService : За данным ИНН не было найдено никаких ККТ");
                    return new ResponseEntity<>("За данным ИНН не было найдено никаких ККТ",
                            HttpStatus.BAD_REQUEST);
                }
            }
        } catch (HttpClientErrorException ex) {
            LOGGER.error("KKTService insertKktFromInn : Запрос не вернул статус 'Success'");
            LOGGER.error("Ошибки:");
            JsonObject err = gson.fromJson(ex.getMessage(), JsonObject.class);
            for (int i = 0; i < err.getAsJsonArray("Errors").size(); i++) {
                LOGGER.error(err.getAsJsonArray("Errors").get(i).getAsString());
            }
            isUpdating.getAndSet(false);
            isErr.getAndSet(true);
            attemptsCount.set(1);
            return new ResponseEntity<>("Запрос не вернул статус 'Success'", HttpStatus.BAD_REQUEST);
        } catch (HttpServerErrorException ex) {
            if (attemptsCount.get() <= 3 && ex.getStatusCode().is5xxServerError()) {
                LOGGER.info("OFD.ru перегружен, попытка [" + (attemptsCount.get()) + "/3]");
                attemptsCount.incrementAndGet();
                isErr.getAndSet(true);
                insertOrUpdateKktFromInn(inn, update, token);
            } else {
                LOGGER.info("Запрос не вернул статус 'Success'");
                isUpdating.getAndSet(false);
                isErr.getAndSet(true);
                attemptsCount.set(1);
                return new ResponseEntity<>("Запрос не вернул статус 'Success'", HttpStatus.BAD_REQUEST);
            }
        }catch (Exception ex){
            LOGGER.error("Произошла ошибка при добавлении ККТ. Подробнее в логах Spring.");
            return new ResponseEntity<>("Ошибка KktService:"+ex.getMessage(),HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Произошла магия, и при добавлении касс метод вернул это сообщение. Как так получилось?...", HttpStatus.BAD_REQUEST);
    }

    public AtomicBoolean getIsUpdating() {
        return isUpdating;
    }

    public AtomicBoolean getIsErr() {
        return isErr;
    }

}
