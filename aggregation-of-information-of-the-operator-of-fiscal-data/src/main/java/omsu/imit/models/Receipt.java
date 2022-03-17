package omsu.imit.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import javax.persistence.*;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EnableAutoConfiguration
@Entity
@Table(name = "receipt", indexes = @Index(name = "receipt_number_index", columnList = "receiptNumber"))
public class Receipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private int receiptNumber; //	Номер документа в смене (по данным кассы)
    private LocalDateTime cDateUtc; //(ISO) Дата и время приема документа в ИС
    private boolean inCorrection; // Имеет значение true, если чек или бланк строгой отчетности (БСО) является документом коррекции, иначе – false
    private LocalDateTime DocDateTime; //Дата и время в формате ISO	Дата и время формирования документа (чека)
    private int docNumber;  //Фискальный номер документа
    private int ShiftNumber;    //Номер смены (по данным кассы), в которую был сформирован документ
    private String OperationType;  //Тип операции
    private int tag;    //	Численный признак вида документа:3 – чек,31 – чек коррекции,4 – бланк строгой отчетности,41 – бланк строгой отчетности коррекции
    private int cashSumm;   //	Сумма по чеку (БСО) электронными в копейках
    private int eCashSumm;  //Сумма по чеку (БСО) электронными в копейках
    private int TotalSumm;   //	Общая сумма по чеку в копейках
    private int depth;    //Количество позиций(?) в чеке
    private String fnsStatus;

    @Column(name = "raw_json", columnDefinition = "longtext")
    private String rawJson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kkt_id", nullable = false)
    @JsonIgnore
    private Kkt kkt;

    public Receipt(int receiptNumber, LocalDateTime cDateUtc, boolean inCorrection, LocalDateTime docDateTime, int docNumber, int shiftNumber, String operationType, int tag, int cashSumm, int eCashSumm, int totalSumm, int depth, String fnsStatus, String rawJson, Kkt kkt) {
        this.receiptNumber = receiptNumber;
        this.cDateUtc = cDateUtc;
        this.inCorrection = inCorrection;
        this.DocDateTime = docDateTime;
        this.docNumber = docNumber;
        ShiftNumber = shiftNumber;
        OperationType = operationType;
        this.tag = tag;
        this.cashSumm = cashSumm;
        this.eCashSumm = eCashSumm;
        TotalSumm = totalSumm;
        this.depth = depth;
        this.fnsStatus = fnsStatus;
        this.rawJson = rawJson;
        this.kkt = kkt;
    }
}
