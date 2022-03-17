package omsu.imit.dto.request;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
@Getter
@Setter
public class OfdTokenRequest {
    @NotNull
    @NotEmpty
    private String Login;
    @NotNull
    @NotEmpty
    private String Password;
}
