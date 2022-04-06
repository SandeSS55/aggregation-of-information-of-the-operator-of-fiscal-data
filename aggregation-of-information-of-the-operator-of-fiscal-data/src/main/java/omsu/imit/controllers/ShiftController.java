package omsu.imit.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import omsu.imit.dto.request.InnInfoRequest;
import omsu.imit.dto.request.OfdTokenRequest;
import omsu.imit.dto.request.ReceiptRequest;
import omsu.imit.dto.request.ReportRequest;
import omsu.imit.models.Inn;
import omsu.imit.services.*;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.Objects;

@RestController
@RequestMapping("/shifts")
public class ShiftController {

    @Autowired
    private KktService kktService;
    @Autowired
    private InnService innService;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private UserService userService;
    @Autowired
    private OfdService ofdService;

    private String login;

    private String password;

    private String[] inn;

    private String startDate;

    private final Logger LOGGER = LoggerFactory.getLogger(ShiftController.class);

    @PostMapping("/addInn")
    public ResponseEntity<String> insertInn(@RequestBody InnInfoRequest innInfoRequest) throws JsonProcessingException {
        ResponseEntity<?> responseEntity = innService.insertInn(innInfoRequest);
        if (responseEntity.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        responseEntity = insertKKTs((long) responseEntity.getBody());
        if (responseEntity.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        responseEntity = insertReceiptsNoUpdate(innInfoRequest);

        return new ResponseEntity<String>((String) responseEntity.getBody(), HttpStatus.OK);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> insertUser(@RequestBody OfdTokenRequest ofdTokenRequest) {
        return userService.addUser(ofdTokenRequest);
    }

    @PostMapping("/reports")
    public ResponseEntity<?> createReport(@RequestBody ReportRequest reportRequest, HttpServletResponse response) throws IOException {
        return ofdService.createXls(LocalDateTime.parse(reportRequest.getFrom()),
                LocalDateTime.parse(reportRequest.getTo()), reportRequest.getKkts(),response);
    }

    @GetMapping(value = "/reports/{filename:.+}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody void getFile(@PathVariable("filename") String file,HttpServletResponse response) throws IOException {
        File out = new File("../reports/" + file);
        if (out.exists()) {

            response.setContentType("application/vnd.ms-excel");
            /*response.setHeader("Content-Disposition", String.format("inline; filename=\"" + out.getName() + "\""));*/
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"" + out.getName() + "\""));
            response.setContentLength((int) out.length());
            InputStream inputStream = new BufferedInputStream(new FileInputStream(out));
            FileCopyUtils.copy(inputStream, response.getOutputStream());
        }
    }

    @GetMapping("/update")
    @Async
    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
    public ResponseEntity<?> updateBase() throws JsonProcessingException {
        for (Inn inn : Objects.requireNonNull(innService.getInfoAboutAllInn().getBody())) {
            kktService.insertOrUpdateKktFromInn(inn.getInn(), true);
            insertReceiptsFromUpdate(inn.getInn());
        }
        return new ResponseEntity<>("Все ИНН, ККТ были успешно обновлены. Добавлены новые чеки.", HttpStatus.OK);
    }

    @GetMapping("/deleteOldReports")
    @Async
    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteOldReports(){
        ofdService.deleteOldReports();
    }

    @PostMapping("/deleteInn")
    public ResponseEntity<?> deleteInn(@RequestBody InnInfoRequest innInfoRequest) {
        LOGGER.info("ИНН : "+innInfoRequest.getInn()+" был успешно удалён из локальной базы данных вместе с ККТ и чеками.");
        return innService.deleteInnByObj(innInfoRequest);
    }

    @PostMapping("/receipts")
    public ResponseEntity<?> getReceipts(@RequestBody ReceiptRequest receiptRequest){
        ResponseEntity<?> responseEntity = new ResponseEntity<>(receiptService.getReceiptsByDate(receiptRequest.getDate(),receiptRequest.getId()).toString(),HttpStatus.OK);
        LOGGER.info("Чеки собраны.");
        return responseEntity;
    }

    public ResponseEntity<?> insertKKTs(long inn) {
        return kktService.insertOrUpdateKktFromInn(inn, false);
    }

    public ResponseEntity<?> insertReceiptsNoUpdate(InnInfoRequest innInfoRequest) throws JsonProcessingException {
        return receiptService.insertReceiptsFromInn(innInfoRequest.getInn(), false, innInfoRequest.getStartFrom());
    }

    public ResponseEntity<?> insertReceiptsFromUpdate(long inn) throws JsonProcessingException {
        return receiptService.insertReceiptsFromInn(inn, true, null);
    }
}
