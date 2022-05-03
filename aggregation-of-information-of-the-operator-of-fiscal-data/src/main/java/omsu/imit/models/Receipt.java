package omsu.imit.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import javax.persistence.*;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@EnableAutoConfiguration
@Getter
@Setter
@Entity
@Table(name = "receipt", indexes = @Index(name = "receipt_number_index", columnList = "receiptNumber"))
public class Receipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int ReceiptNumber; //	Номер документа в смене (по данным кассы)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime CDateUtc; //(ISO) Дата и время приема документа в ИС
    private boolean IsCorrection; // Имеет значение true, если чек или бланк строгой отчетности (БСО) является документом коррекции, иначе – false
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime DocDateTime; //Дата и время в формате ISO	Дата и время формирования документа (чека)
    private int DocNumber;  //Фискальный номер документа
    private int ShiftNumber;    //Номер смены (по данным кассы), в которую был сформирован документ
    private String OperationType;  //Тип операции
    private int Tag;    //	Численный признак вида документа:3 – чек,31 – чек коррекции,4 – бланк строгой отчетности,41 – бланк строгой отчетности коррекции
    private int CashSumm;   //	Сумма по чеку (БСО) электронными в копейках
    private int ECashSumm;  //Сумма по чеку (БСО) электронными в копейках
    private int TotalSumm;   //	Общая сумма по чеку в копейках
    private int Depth;    //Количество позиций(?) в чеке
    private String FnsStatus;

    @Column(name = "raw_json", columnDefinition = "longtext")
    @JsonIgnore
    private String rawJson;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "kkt_id", nullable = false)
    @JsonIgnore
    private Kkt kkt;

    public Receipt(int receiptNumber, LocalDateTime CDateUtc,
                   boolean isCorrection, LocalDateTime docDateTime, int docNumber, int shiftNumber, String operationType,
                   int tag, int cashSumm, int ECashSumm, int totalSumm, int depth, String fnsStatus, String rawJson, Kkt kkt) {
        ReceiptNumber = receiptNumber;
        this.CDateUtc = CDateUtc;
        IsCorrection = isCorrection;
        DocDateTime = docDateTime;
        DocNumber = docNumber;
        ShiftNumber = shiftNumber;
        OperationType = operationType;
        Tag = tag;
        CashSumm = cashSumm;
        this.ECashSumm = ECashSumm;
        TotalSumm = totalSumm;
        Depth = depth;
        FnsStatus = fnsStatus;
        this.rawJson = rawJson;
        this.kkt = kkt;
    }
}
