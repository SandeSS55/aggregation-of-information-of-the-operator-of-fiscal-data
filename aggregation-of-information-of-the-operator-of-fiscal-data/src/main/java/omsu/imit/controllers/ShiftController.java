package omsu.imit.controllers;


import omsu.imit.dto.request.InnInfoRequest;
import omsu.imit.dto.request.OfdTokenRequest;
import omsu.imit.dto.request.ReportRequest;
import omsu.imit.models.Inn;
import omsu.imit.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
    public ResponseEntity<String> insertInn(@RequestBody InnInfoRequest innInfoRequest) {
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
    public ResponseEntity<?> createReport(@RequestBody ReportRequest reportRequest) throws IOException {
        return ofdService.createXls(LocalDateTime.parse(reportRequest.getFrom()),
                LocalDateTime.parse(reportRequest.getTo()), reportRequest.getKkts());
    }

    @GetMapping("/update")
    @Async
    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
    public ResponseEntity<?> updateBase() {
        for (Inn inn : Objects.requireNonNull(innService.getInfoAboutAllInn().getBody())) {
            kktService.insertOrUpdateKktFromInn(inn.getInn(), true);
            insertReceiptsFromUpdate(inn.getInn());
        }
        return new ResponseEntity<>("Все ИНН, ККТ были успешно обновлены. Добавлены новые чеки.", HttpStatus.OK);
    }

    @PostMapping("/deleteInn")
    public ResponseEntity<?> deleteInn(@RequestBody InnInfoRequest innInfoRequest) {
        LOGGER.info("ИНН : "+innInfoRequest.getInn()+" был успешно удалён из локальной базы данных вместе с ККТ и чеками.");
        return innService.deleteInnByObj(innInfoRequest);
    }

    public ResponseEntity<?> insertKKTs(long inn) {
        return kktService.insertOrUpdateKktFromInn(inn, false);
    }

    public ResponseEntity<?> insertReceiptsNoUpdate(InnInfoRequest innInfoRequest) {
        return receiptService.insertReceiptsFromInn(innInfoRequest.getInn(), false, innInfoRequest.getStartFrom());
    }

    public ResponseEntity<?> insertReceiptsFromUpdate(long inn) {
        return receiptService.insertReceiptsFromInn(inn, true, null);
    }
}
