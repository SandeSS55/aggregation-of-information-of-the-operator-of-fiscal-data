package omsu.imit.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import omsu.imit.dto.request.InnInfoRequest;
import omsu.imit.dto.request.OfdTokenRequest;
import omsu.imit.dto.request.ReceiptRequest;
import omsu.imit.dto.request.ReportRequest;
import omsu.imit.models.Inn;
import omsu.imit.models.User;
import omsu.imit.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
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

    private final Logger LOGGER = LoggerFactory.getLogger(ShiftController.class);

    @PostMapping("/addInn")
    public ResponseEntity<String> insertInn(@RequestBody InnInfoRequest innInfoRequest) throws JsonProcessingException {
        ResponseEntity<?> responseEntity = innService.insertInn(innInfoRequest,userService.login(),userService.getUser());
        if (responseEntity.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        responseEntity = insertKKTs((long) responseEntity.getBody());
        if (responseEntity.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        responseEntity = insertReceiptsNoUpdate(innInfoRequest,userService.login());

        return new ResponseEntity<String>((String) responseEntity.getBody(), HttpStatus.OK);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> insertUser(@RequestBody OfdTokenRequest ofdTokenRequest) {
        return userService.addUser(ofdTokenRequest);
    }

    @PostMapping("/reports")
    public ResponseEntity<?> createReport(@RequestBody ReportRequest reportRequest) throws IOException {
        return receiptService.createXls(LocalDateTime.parse(reportRequest.getFrom()),
                LocalDateTime.parse(reportRequest.getTo()), reportRequest.getKkts());
    }

    @GetMapping(value = "/reports/{filename:.+}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody
    void getFile(@PathVariable("filename") String file, HttpServletResponse response) throws IOException {
        File out = new File("../reports/" + file);
        if (out.exists()) {

            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"" + out.getName() + "\""));
            response.setContentLength((int) out.length());
            InputStream inputStream = new BufferedInputStream(new FileInputStream(out));
            FileCopyUtils.copy(inputStream, response.getOutputStream());
        }
    }


    public User getUser(){
        return userService.getUser();
    }

    @GetMapping("/update")
    @Async
    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
    public ResponseEntity<?> updateBase() {
        for (Inn inn : Objects.requireNonNull(innService.getInfoAboutAllInn().getBody())) {
            kktService.insertOrUpdateKktFromInn(inn.getInn(), true,userService.login());
            insertReceiptsFromUpdate(inn.getInn(),userService.login());
        }
        return new ResponseEntity<>("Все ИНН, ККТ были успешно обновлены. Добавлены новые чеки.", HttpStatus.OK);
    }

    @GetMapping("/isUpdate")
    @Async
    public ResponseEntity<?> isBaseUpdating() {
        return new ResponseEntity<>(new boolean[]{kktService.getIsErr().get(), kktService.getIsUpdating().get(), receiptService.getIsErr().get(), receiptService.getIsUpdating().get()}, HttpStatus.OK);
    }

    @GetMapping("/deleteOldReports")
    @Async
    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteOldReports() {
        ofdService.deleteOldReports();
    }

    @PostMapping("/deleteInn")
    public ResponseEntity<?> deleteInn(@RequestBody InnInfoRequest innInfoRequest) {
        Inn inn = innService.getInfoAboutCertainInn(innInfoRequest.getInn()).getBody();
        Objects.requireNonNull(inn).getKktSet().forEach(s -> receiptService.deleteAllReceiptByKkt(s.getId()));
        kktService.deleteAllKktByInn(inn.getId());
        LOGGER.info("ИНН : " + innInfoRequest.getInn() + " был успешно удалён из локальной базы данных вместе с ККТ и чеками.");
        return innService.deleteInnByObj(innInfoRequest);
    }

    @PostMapping("/receipts")
    public ResponseEntity<?> getReceipts(@RequestBody ReceiptRequest receiptRequest) {
        ResponseEntity<?> responseEntity = new ResponseEntity<>(
                receiptService.getReceiptsByDate(receiptRequest.getDate(),kktService.getKktByid(receiptRequest.getId())).toString(), HttpStatus.OK);
        LOGGER.info("Чеки собраны.");
        return responseEntity;
    }

    public ResponseEntity<?> insertKKTs(long inn) {
        return kktService.insertOrUpdateKktFromInn(inn, false,userService.login());
    }

    public ResponseEntity<?> insertReceiptsNoUpdate(InnInfoRequest innInfoRequest, String token) {
        return receiptService.insertReceiptsFromInn(innInfoRequest.getInn(), false, token, innInfoRequest.getStartFrom(),
                kktService.getAllKktByInn(innInfoRequest.getInn()));
    }

    public ResponseEntity<?> insertReceiptsFromUpdate(long inn, String token) {
        return receiptService.insertReceiptsFromInn(inn, true, token, null,kktService.getAllKktByInn(inn));
    }
}
