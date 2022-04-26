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
import omsu.imit.interfaces.HttpRequest;
import omsu.imit.models.Kkt;
import omsu.imit.models.Receipt;
import omsu.imit.repo.ReceiptsCrudRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ReceiptService {

    @Autowired
    private ReceiptsCrudRepository receiptsCrudRepository;

    @Autowired
    private HttpRequest httpRequest;

    ObjectMapper mapper = JsonMapper.builder()
            .addModule(new ParameterNamesModule())
            .addModule(new Jdk8Module())
            .addModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    Gson gson = new Gson();

    /**
     * Следующие два булева необходимы, чтобы
     * Правильно отображать в UI состояние
     * Базы: Обновлена, обновляется, произошла ошибка при обновлении
     */
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    private final AtomicBoolean isErr = new AtomicBoolean(false);

    private final AtomicInteger amountOfTries = new AtomicInteger(1);

    private final Logger LOGGER = LoggerFactory.getLogger(ReceiptService.class);

    public void deleteAllReceiptByKkt(long kkt) {
        receiptsCrudRepository.deleteAllReceiptsByKkt(kkt);
    }

    public List<Receipt> findAllReceipt() {
        return StreamSupport.stream(receiptsCrudRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    public JsonArray getReceiptsByDate(String date, Optional<Kkt> kkt) {
        List<Kkt> kkts = new ArrayList<>();

        if (kkt.isEmpty()) {
            return new JsonArray();
        }

        kkts.add(kkt.get());
        LocalDateTime from = LocalDate.parse(date).atTime(0, 0, 0);

        List<Receipt> list = findAllReceipt().stream().filter
                (s -> kkts.contains(s.getKkt())).filter(s -> s.getDocDateTime().isAfter(from) && s.getDocDateTime().isBefore(from.plusDays(1))).collect(Collectors.toList());

        JsonArray obj = new JsonArray();
        list.forEach(s -> {
            try {
                obj.add(gson.fromJson(mapper.writer().withDefaultPrettyPrinter().writeValueAsString(s), JsonObject.class));
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
        return arr;
    }

    public ResponseEntity<?> insertReceiptsFromInn(long inn, boolean update,String token, LocalDateTime startFrom,List<Kkt> kktList) {
        try {
            getIsUpdating().getAndSet(true);
            List<Receipt> receipts = new ArrayList<>();
            for (Kkt kkt : kktList) {
                LocalDateTime from;
                if (!update && startFrom == null) {
                    from = kkt.getFirstDocumentDate();
                } else if (!update) {
                    from = startFrom;
                } else {
                    if (kkt.getLastTimeUpdated() == null) {
                        from = kkt.getFirstDocumentDate();
                    } else {
                        from = kkt.getLastTimeUpdated();
                    }
                }
                LocalDateTime to = kkt.getLastDocOnOfdDateTime();
                while (from.isBefore(to)) {
                    ResponseEntity<String> responseEntity = httpRequest.getPostsPlainJSON(
                            "https://ofd.ru/api/integration/v1/inn/" + inn + "/kkt/" + kkt.getKktRegNumber() +
                                    "/receipts-with-fpd-short?dateFrom=" + from.toString() +
                                    "&dateTo=" + from.plusDays(90) + "&AuthToken=" + token
                    );
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
            getIsErr().getAndSet(false);
            getIsUpdating().getAndSet(false);
            if (!update) {
                LOGGER.info("Все чеки были успешно загружены в базу данных. Общее количество чеков: " + receiptsCrudRepository.count());
                amountOfTries.set(1);
                return new ResponseEntity<>("Все чеки были успешно загружены в базу данных. Общее количество чеков: " +
                        receiptsCrudRepository.count(), HttpStatus.OK);
            } else {
                LOGGER.info("Все чеки были успешно загружены в базу данных во время обновления базы." +
                        " Общее количество чеков: " + receiptsCrudRepository.count());
                amountOfTries.set(1);
                return new ResponseEntity<>("Все чеки были успешно загружены в базу данных во время обновления базы." +
                        " Общее количество чеков: " + receiptsCrudRepository.count(), HttpStatus.OK);
            }
        } catch (HttpClientErrorException ex) {
            LOGGER.error("ReceiptService insertReceiptsFromInn : Ошибка при отправке запроса на чеки");
            LOGGER.error("Ошибки:");
            for (int j = 0; j < gson.fromJson(ex.getMessage(), JsonObject.class).getAsJsonArray("Errors").size(); j++) {
                LOGGER.error(gson.fromJson(ex.getMessage(), JsonObject.class).getAsJsonArray("Errors").get(j).getAsString());
            }
            getIsUpdating().getAndSet(false);
            amountOfTries.set(1);
            return new ResponseEntity<>("insertReceiptsFromInn : Ошибка при отправке запроса на чеки", HttpStatus.BAD_REQUEST);
        }catch (HttpServerErrorException ex){
            if (amountOfTries.get() <= 3) {
                amountOfTries.getAndIncrement();
                getIsErr().getAndSet(true);
                insertReceiptsFromInn(inn, update,token, startFrom,kktList);
            } else {
                getIsUpdating().getAndSet(false);
                amountOfTries.set(1);
                return new ResponseEntity<>("insertReceiptsFromInn : Ошибка при отправке запроса на чеки", HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<>("Произошла магия, и при добавлении чеков метод вернул это сообщение. Как так получилось?...", HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<?> createXls(LocalDateTime from, LocalDateTime to, List<String> kkts) throws IOException {

        if (kkts.size() < 1) {
            LOGGER.error("В базе не найдено ККТ для создания отчётов.");
            return new ResponseEntity<>("В базе не найдено ККТ для создания отчётов.", HttpStatus.OK);
        }

        List<Receipt> receiptList = findAllReceipt().stream().filter
                (s -> kkts.contains(s.getKkt().getKktRegNumber())).filter(s -> s.getDocDateTime().isAfter(from) && s.getDocDateTime().isBefore(to)).
                sorted(((Comparator<Receipt>) (o1, o2) -> {
                    return Long.compare(Long.parseLong(o1.getKkt().getKktRegNumber()), Long.parseLong(o2.getKkt().getKktRegNumber()));
                }).thenComparingInt(Receipt::getDocNumber)).collect(Collectors.toList());

        if (!Files.exists(Paths.get("../reports"), LinkOption.NOFOLLOW_LINKS)) {
            if (new File("../reports").mkdirs()) {
                LOGGER.info("Папка 'reports' была успешна создана в корневой папке сервиса OfdToExcel");
            }
        }

        XSSFWorkbook workbook = new XSSFWorkbook();

        Sheet shift = workbook.createSheet("Отчёт");

        Row header = shift.createRow(0);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFFont font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(font);

        String[] data = {"Касса", "Адрес кассы", "Дата", "Фискальный Номер", "Вид Документа", "Вид оплаты", "Сумма"};

        for (int i = 0; i < data.length; i++) {
            Cell headerCell = header.createCell(i);
            headerCell.setCellValue(data[i]);
            headerCell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < 7; i++) {
            shift.setColumnWidth(i, 9000);
        }

        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);

        if (receiptList.size() < 1) {
            LOGGER.error("В базе не найдено чеков для создания отчётов за текущий период.");
            return new ResponseEntity<>("В базе не найдено чеков для создания отчётов за текущий период.", HttpStatus.OK);
        } else {
            for (int i = 0; i < receiptList.size(); i++) {
                Row row = shift.createRow(i + 1);
                String doc = "";
                switch (receiptList.get(i).getOperationType()) {
                    case ("Income"):
                        doc = "Приход";
                        break;
                    case ("Expense"):
                        doc = "Расход";
                        break;
                    case ("Refund income"):
                        doc = "Возврат прихода";
                        break;
                    case ("Refund expense"):
                        doc = "Возврат расхода";
                        break;
                }
                String[] out = {
                        receiptList.get(i).getKkt().getKktRegNumber(),
                        receiptList.get(i).getKkt().getFiscalAddress(),
                        receiptList.get(i).getDocDateTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")),
                        String.valueOf(receiptList.get(i).getDocNumber()),
                        doc,
                        "Оплата наличными",
                        String.valueOf(receiptList.get(i).getTotalSumm() / 100)
                };
                if (receiptList.get(i).getCashSumm() < receiptList.get(i).getECashSumm()) {
                    out[5] = "Безналичная оплата";
                }
                for (int j = 0; j < out.length; j++) {
                    Cell cell = row.createCell(j);
                    if (j == 6) {
                        cell.setCellValue(Double.parseDouble(out[j]));
                    } else {
                        cell.setCellValue(out[j]);
                    }
                    cell.setCellStyle(style);
                }
            }
            File currDir = new File("../reports/");
            String path = currDir.getCanonicalPath();

            String fileName = "report-" + LocalDateTime.now().
                    format(DateTimeFormatter.ofPattern("dd-MM-yyyyHH:mm:ss")).replace(':', '-') + ".xlsx";

            String fileLocation = path + "\\" + fileName;

            FileOutputStream outputStream = new FileOutputStream(fileLocation);
            workbook.write(outputStream);
            workbook.close();
            LOGGER.info("Отчет был успешно создан : " + path);
            return new ResponseEntity<>(new String[]{"Отчет был успешно создан : " + fileLocation, fileName}, HttpStatus.OK);
        }
    }

    public AtomicBoolean getIsUpdating() {
        return isUpdating;
    }

    public AtomicBoolean getIsErr() {
        return isErr;
    }

}
