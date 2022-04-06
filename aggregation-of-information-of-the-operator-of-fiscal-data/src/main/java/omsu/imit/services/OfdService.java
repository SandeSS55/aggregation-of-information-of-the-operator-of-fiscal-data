package omsu.imit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import omsu.imit.models.Receipt;
import omsu.imit.models.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class OfdService {
    private final RestTemplate restTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private ReceiptService receiptService;

    Gson gson = new Gson();

    private final Logger LOGGER = LoggerFactory.getLogger(OfdService.class);

    public OfdService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public ResponseEntity<String> getPostsPlainJSON(String url) {
        try {
            return this.restTemplate.getForEntity(url, String.class);
        } catch (HttpClientErrorException ex) {
            return new ResponseEntity<>(ex.getMessage(),ex.getStatusCode());
        }
    }

    public JsonObject loginPostPlainJSON() {
        User user = userService.getUser();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Map<String, Object> map = new HashMap<>();
        map.put("Login", user.getLogin());
        map.put("Password", user.getPassword());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<String> jsonObjectResponseEntity = this.restTemplate.postForEntity
                    ("https://ofd.ru/api/Authorization/CreateAuthToken", entity, String.class);
            if (Objects.requireNonNull(jsonObjectResponseEntity).getStatusCode() == HttpStatus.OK) {
                return gson.fromJson(jsonObjectResponseEntity.getBody(), JsonObject.class);
            } else {
                JsonArray body = gson.fromJson(jsonObjectResponseEntity.getBody(), JsonObject.class).getAsJsonArray("Errors");
                LOGGER.error("Токен не был создан/обновлён по причине(ам):");
                for (int i = 0; i < gson.fromJson(jsonObjectResponseEntity.getBody(), JsonObject.class).size(); i++) {
                    LOGGER.error(body.get(i).getAsString());
                }
                return null;
            }
        } catch (HttpClientErrorException ex) {
            return null;
        }
    }

    public ResponseEntity<?> createXls(LocalDateTime from, LocalDateTime to, List<String> kkts) throws IOException {

        if (kkts.size() < 1) {
            LOGGER.error("В базе не найдено ККТ для создания отчётов.");
            return new ResponseEntity<>("В базе не найдено ККТ для создания отчётов.", HttpStatus.OK);
        }

        List<Receipt> receiptList = receiptService.findAllReceipt().stream().filter
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

            String fileLocation = path + "\\" + "report-" + LocalDateTime.now().
                    format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")).replace(':', '-') + ".xlsx";

            FileOutputStream outputStream = new FileOutputStream(fileLocation);
            workbook.write(outputStream);
            workbook.close();
            LOGGER.info("Отчет был успешно создан : " + path);
            return new ResponseEntity<>("Отчет был успешно создан : " + path, HttpStatus.OK);
        }
    }
}
