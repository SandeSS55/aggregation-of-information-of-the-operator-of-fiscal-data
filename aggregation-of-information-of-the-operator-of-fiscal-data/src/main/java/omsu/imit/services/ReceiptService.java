package omsu.imit.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

    ObjectMapper mapper = JsonMapper.builder() // or different mapper for other format
            .addModule(new ParameterNamesModule())
            .addModule(new Jdk8Module())
            .addModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // and possibly other configuration, modules, then:
            .build();

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

    public JsonArray getReceiptsByDate(String date, Long id) {
        List<Optional<Kkt>> kkts = new ArrayList<>();
        if (Objects.isNull(kktService.getKktByid(id))) {
            return new JsonArray();
        }
        kkts.add(Optional.of(kktService.getKktByid(id).get()));
        LocalDateTime from = LocalDate.parse(date).atTime(0, 0, 0);

        List<Receipt> list = new ArrayList<>(findAllReceipt().stream().filter
                (s -> kkts.contains(Optional.ofNullable(s.getKkt()))).filter(s -> s.getDocDateTime().isAfter(from) && s.getDocDateTime().isBefore(from.plusDays(1))).collect(Collectors.toList()));

        JsonArray obj = new JsonArray();
        list.forEach(s-> {
            try {
                obj.add(gson.fromJson(mapper.writer().withDefaultPrettyPrinter().writeValueAsString(s),JsonObject.class));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
        JsonArray arr = new JsonArray();
        obj.forEach(s -> {
            String tmp;
            JsonObject json = s.getAsJsonObject();
            json.remove("id");
            switch (Integer.parseInt(json.get("tag").getAsString())) {
                case 3:
                    tmp = "Чек";
                    break;
                case 31:
                    tmp = "Чек Коррекции";
                    break;
                case 4:
                    tmp = "Бланк строгой отчетности";
                    break;
                case 41:
                    tmp = "Бланк строгой отчетности коррекции";
                    break;
                default:
                    tmp = "";
                    break;
            }
            json.addProperty("Вид", tmp);
            json.remove("tag");
            json.add("КолПоз", json.get("depth"));
            json.remove("depth");
            json.add("ФискНомДок", json.get("docNumber"));
            json.remove("docNumber");
            switch (json.get("operationType").getAsString()) {
                case "Income":
                    tmp = "Приход";
                    break;
                case "Расход":
                    tmp = "Вид";
                    break;
                case "Refund income":
                    tmp = "возврат прихода";
                    break;
                case "Refund expense":
                    tmp = "возврат расхода";
                    break;
                default:
                    tmp = "";
                    break;
            }
            json.addProperty("ТипОпер", tmp);
            json.remove("operationType");
            json.add("Безнал", json.get("ecashSumm"));
            json.remove("ecashSumm");
            json.add("Наличными", json.get("cashSumm"));
            json.remove("cashSumm");
            json.addProperty("ВремяЧека", LocalDateTime.parse(json.get("docDateTime").getAsString()).format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
            json.remove("docDateTime");
            json.add("ИтоговаяСумма", json.get("totalSumm"));
            json.remove("totalSumm");
            json.add("НомерСмены", json.get("shiftNumber"));
            json.remove("shiftNumber");
            switch (json.get("fnsStatus").getAsString()) {
                case "Success":
                    tmp = "Успех";
                    break;
                case "Failed":
                    tmp = "Неудача";
                    break;
                default:
                    tmp = "";
                    break;
            }
            json.addProperty("СтатусФНС", tmp);
            json.remove("fnsStatus");
            json.add("НомЧекСмена", json.get("receiptNumber"));
            json.remove("receiptNumber");
            switch (json.get("isCorrection").getAsString()) {
                case "true":
                    tmp = "Да";
                    break;
                case "false":
                    tmp = "Нет";
                    break;
                default:
                    tmp = "";
                    break;
            }
            json.addProperty("ЧекКорр?", tmp);
            json.remove("isCorrection");
            json.remove("cDateUtc");
            json.remove("CDateUtc");
            json.remove("cdateUtc");
            json.remove("DocDateTime");
            arr.add(json);
        });
        String str = arr.toString();
        return arr;
    }

    public ResponseEntity<?> insertReceiptsFromInn(long inn, boolean update, LocalDateTime startFrom) {
        List<Kkt> kktList = kktService.getAllKktByInn(inn);
        String token = userService.login();
        List<Receipt> receipts = new ArrayList<>();
        for (Kkt kkt : kktList) {
            LocalDateTime from;
            if (!update && startFrom==null) {
                from = kkt.getFirstDocumentDate();
            }else if(!update){
                from=startFrom;
            }
            else {
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
            LOGGER.info("Чеки для ККТ с регистрационным номером: " + kkt.getKktRegNumber() + " были успешно подготовлены для добавления в базу данных.");
        }
        receipts.sort(Comparator.comparingInt(Receipt::getShiftNumber).thenComparing(Receipt::getDocDateTime).thenComparingLong(o -> o.getKkt().getId()));
        LOGGER.info("Время добавить все чеки!");
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
