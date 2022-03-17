package omsu.imit.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EnableAutoConfiguration
@Entity
@Table(name = "kkt", indexes = @Index(name = "kkt_reg_number_index", columnList = "kktRegNumber"))
public class Kkt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(unique = true)
    @NotNull
    @NotEmpty
    private String fnNumber;    // номер фискального накопителя
    @NotNull
    @NotEmpty
    private String FnEndDate;   //(ISO) Дата и время окончания работы фискального накопителя
    @Column(unique = true)
    @NotNull
    @NotEmpty
    private String kktNumber;   // заводской номер кассы
    @Column(unique = true)
    @NotNull
    @NotEmpty
    private String kktRegNumber;    // Регистрационный номер ККТ (кассы)

    private LocalDateTime FirstDocumentDate;    //Время первого чека в кассе

    private LocalDateTime LastDocOnOfdDateTime;  //Время последнего чека кассы в базе OFD

    private LocalDateTime LastTimeUpdated; //(ISO) Дата последнего обновления чеков кассы в sql

    private String FiscalAddress; //Адрес

    private String FiscalPlace; //Место расчётов

    private String KktModel; //Модель ККТ

    @OneToMany(mappedBy = "kkt", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<Receipt> receiptSet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inn_id", nullable = false)
    @JsonBackReference
    private Inn inn;

    public Kkt(@NotNull @NotEmpty String fnNumber,
               @NotNull @NotEmpty String fnEndDate,
               @NotNull @NotEmpty String kktNumber,
               @NotNull @NotEmpty String kktRegNumber,
               LocalDateTime firstDocumentDate,
               LocalDateTime lastDocOnOfdDateTime,
               LocalDateTime lastTimeUpdated,
               String fiscalAddress,
               String fiscalPlace,
               String kktModel,
               Inn inn) {
        this.fnNumber = fnNumber;
        FnEndDate = fnEndDate;
        this.kktNumber = kktNumber;
        this.kktRegNumber = kktRegNumber;
        FirstDocumentDate = firstDocumentDate;
        LastDocOnOfdDateTime = lastDocOnOfdDateTime;
        LastTimeUpdated = lastTimeUpdated;
        FiscalAddress = fiscalAddress;
        FiscalPlace = fiscalPlace;
        KktModel = kktModel;
        this.inn = inn;
    }
}
