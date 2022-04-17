package omsu.imit.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import omsu.imit.dto.request.InnInfoRequest;
import omsu.imit.models.Inn;
import omsu.imit.repo.InnCrudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

@Service
@Getter
@Setter
public class InnService {
    @Autowired
    private InnCrudRepository innCrudRepository;

    @Autowired
    private KktService kktService;

    @Autowired
    private OfdService ofdService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReceiptService receiptService;

    int errCount;

    Gson gson = new Gson();

    private final Logger LOGGER = LoggerFactory.getLogger(InnService.class);

    public ResponseEntity<Inn> getInfoAboutCertainInn(long inn) {
        return new ResponseEntity<>(innCrudRepository.findByInn(inn), HttpStatus.OK);
    }

    public ResponseEntity<List<Inn>> getInfoAboutAllInn() {
        return new ResponseEntity<>(innCrudRepository.findAll(), HttpStatus.OK);
    }

    public long amountOfInn() {
        return innCrudRepository.count();
    }

    public ResponseEntity<String> deleteInnByObj(InnInfoRequest innInfoRequest) {
        if (innInfoRequest.getInn() <= 0 || innCrudRepository.findByInn(innInfoRequest.getInn()) == null) {
            return new ResponseEntity<>("Данные введены неправильно!", HttpStatus.BAD_REQUEST);
        }
        Inn inn = innCrudRepository.findByInn(innInfoRequest.getInn());
        inn.getKktSet().forEach(s -> receiptService.deleteAllReceiptByKkt(s.getId()));
        kktService.deleteAllKktByInn(inn.getId());
        innCrudRepository.delete(innInfoRequest.getInn());
        return new ResponseEntity<>("Удаление ИНН по объекту прошло успешно!", HttpStatus.OK);
    }

    public ResponseEntity<String> deleteAllInn() {
        innCrudRepository.deleteAll();
        return new ResponseEntity<>("Удаление всех ИНН из базы прошло успешно!", HttpStatus.OK);
    }

    public ResponseEntity<String> deleteInnById(long id) {
        if (innCrudRepository.findById(id).isEmpty()) {
            return new ResponseEntity<>("ИНН по данному номеру не найден!", HttpStatus.BAD_REQUEST);
        }
        innCrudRepository.deleteById(id);
        return new ResponseEntity<>("ИНН был успешно удалён!", HttpStatus.OK);

    }

    public ResponseEntity<?> insertInn(InnInfoRequest innInfoRequest) {

        if (innInfoRequest.getInn() <= 0 || innInfoRequest.getName() == null || innInfoRequest.getName().isEmpty()) {
            return new ResponseEntity<>("Данные введены неправильно!", HttpStatus.BAD_REQUEST);
        }

        if (innCrudRepository.findByInn(innInfoRequest.getInn()) != null) {
            return new ResponseEntity<>("Такой ИНН уже есть в базе!", HttpStatus.BAD_REQUEST);
        }

        try {
            ResponseEntity<String> responseEntity = ofdService.getPostsPlainJSON("https://ofd.ru/api/integration/v1/inn/" +
                    innInfoRequest.getInn() + "/kkts?AuthToken=" + userService.login());
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                if (innInfoRequest.getStartFrom() != null) {
                    innCrudRepository.insertNewInn(innInfoRequest.getInn(), innInfoRequest.getName(), innInfoRequest.getStartFrom(), userService.getUser());
                } else {
                    innCrudRepository.insertNewInn(innInfoRequest.getInn(), innInfoRequest.getName(), null, userService.getUser());
                }
                LOGGER.info("ИНН:" + innInfoRequest.getInn() + " был успешно добавлен в базу, но привязанные ККТ и Чеки в процессе добавления.");

                return new ResponseEntity<>(innInfoRequest.getInn(), HttpStatus.OK);
            }
        } catch (HttpClientErrorException ex) {
            if (gson.fromJson(ex.getMessage(), JsonObject.class).has("Errors") && gson.fromJson(ex.getMessage(), JsonObject.class).getAsJsonArray("Errors").get(0).getAsString().equals("InnNotFound")) {
                LOGGER.error("InnService insertUser : ИНН не найден на данной учётной записи!");
                return new ResponseEntity<>("Такой ИНН не закреплён за данной учётной записью!", HttpStatus.BAD_REQUEST);
            }
            if (gson.fromJson(ex.getMessage(), JsonObject.class).has("Data") && gson.fromJson(ex.getMessage(), JsonObject.class).getAsJsonArray("Data").size() < 1) {
                LOGGER.error("InnService insertUser : У данного ИНН не было найдено ни одного ККТ в базе");
                return new ResponseEntity<>(" У данного ИНН не было найдено ни одного ККТ в базе", HttpStatus.BAD_REQUEST);
            }

            if (ex.getStatusCode().isError())
                if (getErrCount() < 3) {
                    LOGGER.info("OFD.ru перегружен, попытка ["+(getErrCount()+1)+"/3]");
                    setErrCount(getErrCount()+1);
                    return insertInn(innInfoRequest);
                } else {
                    LOGGER.error("OFD.ru перегружен или не отвечает долгое время. Нужно повторить попытку позже. Либо, я не уследил за ошибкой, и вы неверно ввели данные...");
                }
        }
        return null;
    }
}
