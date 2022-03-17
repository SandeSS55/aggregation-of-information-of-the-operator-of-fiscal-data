package omsu.imit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import omsu.imit.models.Kkt;
import omsu.imit.models.Receipt;
import omsu.imit.repo.ReceiptsCrudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ReceiptService {

    @Autowired
    private ReceiptsCrudRepository receiptsCrudRepository;

    @Autowired
    private KktService kktService;

    @Autowired
    private UserService userService;

    @Autowired
    private OfdService ofdService;

    Gson gson = new Gson();

    private final Logger LOGGER = LoggerFactory.getLogger(ReceiptService.class);

    public Receipt getInfoAboutCertainReceipt(long id) {
        return receiptsCrudRepository.findById(id).orElse(null);
    }

    public List<Receipt> getAllReceiptsByKkt(long id) {
        return receiptsCrudRepository.findByKkt(id);
    }

    public long amountOfReceipts() {
        return receiptsCrudRepository.count();
    }

    public void deleteAllReceipts() {
        receiptsCrudRepository.deleteAll();
    }

    public void deleteReceiptById(long id) {
        receiptsCrudRepository.deleteById(id);
    }

    public void deleteAllReceiptByKkt(long kkt) {
        receiptsCrudRepository.deleteAllReceiptsByKkt(kkt);
    }

    public List<Receipt> findAllReceipt() {
        return StreamSupport.stream(receiptsCrudRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    public ResponseEntity<?> insertReceiptsFromInn(long inn, boolean update, LocalDateTime startFrom) {
        List<Kkt> kktList = kktService.getAllKktByInn(inn);
        String token = userService.login();
        List<Receipt> receipts = new ArrayList<>();
        for (Kkt kkt : kktList) {
            LocalDateTime from;
            if (!update) {
                from = kkt.getFirstDocumentDate();
            } else {
                if (kkt.getLastTimeUpdated() == null) {
                    from = kkt.getFirstDocumentDate();
                } else {
                    from = kkt.getLastTimeUpdated();
                }
            }
            LocalDateTime to = kkt.getLastDocOnOfdDateTime();
            while (from.isBefore(to)) {
                ResponseEntity<String> responseEntity = ofdService.getPostsPlainJSON(
                        "https://ofd.ru/api/integration/v1/inn/" + inn + "/kkt/" + kkt.getKktRegNumber() +
                                "/receipts-with-fpd-short?dateFrom=" + from.toString() +
                                "&dateTo=" + from.plusDays(90) + "&AuthToken=" + token
                );
                if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                    LOGGER.error("ReceiptService insertReceiptsFromInn : Ошибка при отправке запроса на чеки");
                    LOGGER.error("Ошибки:");
                    for (int j = 0; j < gson.fromJson(responseEntity.getBody(), JsonObject.class).getAsJsonArray("Errors").size(); j++) {
                        LOGGER.error(gson.fromJson(responseEntity.getBody(), JsonObject.class).getAsJsonArray("Errors").get(j).getAsString());
                    }
                    return new ResponseEntity<>("ReceiptService insertReceiptsFromInn : Ошибка при отправке запроса на чеки", HttpStatus.BAD_REQUEST);
                }
                JsonArray receiptsRaw = gson.fromJson(responseEntity.getBody(), JsonObject.class).getAsJsonArray("Data");
                receiptsRaw.forEach(s -> {
                    JsonObject obj = gson.fromJson(s, JsonObject.class);
                    receipts.add(new Receipt(
                            Integer.parseInt(obj.get("ReceiptNumber").getAsString()),
                            LocalDateTime.parse(obj.get("CDateUtc").getAsString()),
                            Boolean.parseBoolean(obj.get("IsCorrection").getAsString()),
                            LocalDateTime.parse(obj.get("DocDateTime").getAsString()),
                            Integer.parseInt(obj.get("DocNumber").getAsString()),
                            Integer.parseInt(obj.get("DocShiftNumber").getAsString()),
                            obj.get("OperationType").getAsString(),
                            Integer.parseInt(obj.get("Tag").getAsString()),
                            Integer.parseInt(obj.get("CashSumm").getAsString()),
                            Integer.parseInt(obj.get("ECashSumm").getAsString()),
                            Integer.parseInt(obj.get("TotalSumm").getAsString()),
                            Integer.parseInt(obj.get("Depth").getAsString()),
                            obj.get("FnsStatus").getAsString(),
                            obj.toString(),
                            kkt
                    ));
                });
                from = from.plusDays(90);
            }
            kkt.setLastTimeUpdated(kkt.getLastDocOnOfdDateTime());
            LOGGER.info("Чеки для ККТ с регистрационным номером: " + kkt.getKktRegNumber() + " были успешно добавленны в базу данных.");
        }
        receipts.sort(Comparator.comparingInt(Receipt::getShiftNumber).thenComparing(Receipt::getDocDateTime).thenComparingLong(o -> o.getKkt().getId()));
        receiptsCrudRepository.saveAll(receipts);
        if (!update) {
            LOGGER.info("Все чеки были успешно загружены в базу данных. Общее количество чеков: " + receiptsCrudRepository.count());
            return new ResponseEntity<>("Все чеки были успешно загружены в базу данных. Общее количество чеков: " +
                    receiptsCrudRepository.count(), HttpStatus.OK);
        } else {
            LOGGER.info("Все чеки были успешно загружены в базу данных во время обновления базы." +
                    " Общее количество чеков: " + receiptsCrudRepository.count());
            return new ResponseEntity<>("Все чеки были успешно загружены в базу данных во время обновления базы." +
                    " Общее количество чеков: " + receiptsCrudRepository.count(), HttpStatus.OK);
        }
    }
}
