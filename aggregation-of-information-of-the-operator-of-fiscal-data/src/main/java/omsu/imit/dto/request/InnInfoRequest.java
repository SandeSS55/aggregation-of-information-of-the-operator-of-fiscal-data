package omsu.imit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InnInfoRequest {
    private String name;
    @Size(min = 1)
    private long inn;
    private LocalDateTime startFrom;
}
