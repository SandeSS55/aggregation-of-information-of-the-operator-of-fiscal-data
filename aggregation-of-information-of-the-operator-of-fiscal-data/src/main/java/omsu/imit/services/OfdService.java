package omsu.imit.services;

import omsu.imit.interfaces.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;


@Service
public class OfdService implements HttpRequest {
    private final RestTemplate restTemplate;

    private final Logger LOGGER = LoggerFactory.getLogger(OfdService.class);

    public OfdService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public ResponseEntity<String> getPostsPlainJSON(String url) {
        try {
            return this.restTemplate.getForEntity(url, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            return new ResponseEntity<>(ex.getMessage(), ex.getStatusCode());
        }
    }

    @Async
    public void deleteOldReports() {
        File file = new File("../reports/");
        List<File> list = new ArrayList<>();
        for (String str : Objects.requireNonNull(file.list())) {
            list.add(new File("../reports/" + str));
        }
        AtomicInteger count = new AtomicInteger();
        list.forEach(s -> {
            try {
                FileTime creationTime = (FileTime) Files.getAttribute(Path.of(s.getCanonicalPath()), "creationTime");
                if (LocalDateTime.parse(creationTime.toString().substring(0, 19)).isBefore(LocalDateTime.now().minusDays(1))) {
                    if (s.delete()) {
                        count.getAndIncrement();
                    }
                }
            } catch (IOException e) {
                LOGGER.info(e.getLocalizedMessage());
            }
        });
        LOGGER.info("Очистка директории от устаревших файлов прошла успешно. Удалённых файлов: " + count.toString());
    }
}
