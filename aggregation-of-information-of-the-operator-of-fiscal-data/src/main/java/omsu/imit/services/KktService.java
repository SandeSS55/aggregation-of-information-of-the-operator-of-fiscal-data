package omsu.imit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import omsu.imit.models.Inn;
import omsu.imit.models.Kkt;
import omsu.imit.repo.KktCrudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Service
public class KktService {

    Gson gson = new Gson();

    @Autowired
    private KktCrudRepository kktCrudRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private OfdService ofdService;

    @Autowired
    private InnService innService;

    private final Logger LOGGER = LoggerFactory.getLogger(KktService.class);

    public Kkt getInfoAboutCertainKkt(String kkt) {
        return kktCrudRepository.findByKktRegNumber(kkt);
    }

    public List<Kkt> getAllKktByInn(long inn) {

        return kktCrudRepository.getKkts(Objects.requireNonNull(innService.getInfoAboutCertainInn(inn).getBody()).getId());
    }

    public Optional<Kkt> getKktByid(long id){
        return kktCrudRepository.findById(id);
    }

    public void deleteAllKktByInn(long inn) {
        kktCrudRepository.deleteKktByInn(inn);
    }

    public long amountOfKkt() {
        return kktCrudRepository.count();
    }

    public void deleteAllKkt() {
        kktCrudRepository.deleteAll();
    }

    public void insertKkt(String fnNumber, String kktNumber, String kktRegNumber,
                          LocalDateTime fn_end_time, LocalDateTime firstDoc, LocalDateTime lastDoc, LocalDateTime date2, long inn) {
        kktCrudRepository.insertKkt(fnNumber, kktNumber, kktRegNumber, fn_end_time,
                firstDoc, lastDoc, date2, inn);
    }

    public ResponseEntity<?> insertOrUpdateKktFromInn(long inn, boolean update) {
        ResponseEntity<String> responseEntity = ofdService.getPostsPlainJSON
                ("https://ofd.ru/api/integration/v1/inn/" + inn + "/kkts?AuthToken=+" + userService.login());
        Inn main = innService.getInfoAboutCertainInn(inn).getBody();
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
                                    kktCrudRepository.updateKkt(s.getFnNumber(), s.getFnEndDate(), s.getFiscalAddress(), s.getFiscalPlace(),s.getLastDocOnOfdDateTime().toString(), Long.parseLong(s.getKktRegNumber()));
                                } else {
                                    kktCrudRepository.save(s);
                                }
                            }
                    );
                }
                LOGGER.info("ККТ были успешно обновлены в базе, очередь чеков.");
                return new ResponseEntity<>("ККТ были успешно обновлены в базе, очередь чеков.", HttpStatus.OK);
            } else {
                LOGGER.error("KKTService : За данным ИНН не было найдено никаких ИНН");
                return new ResponseEntity<>("За данным ИНН не было найдено никаких ИНН", HttpStatus.BAD_REQUEST);
            }
        } else {
            LOGGER.error("KKTService insertKktFromInn : Запрос не вернул статус 'Success'");
            LOGGER.error("Ошибки:");
            for (int i = 0; i < gson.fromJson(responseEntity.getBody(), JsonObject.class).getAsJsonArray("Errors").size(); i++) {
                LOGGER.error(gson.fromJson(responseEntity.getBody(), JsonObject.class).getAsJsonArray("Errors").get(i).getAsString());
            }
            return new ResponseEntity<>("Запрос не вернул статус 'Success'", HttpStatus.BAD_REQUEST);
        }
    }
}
