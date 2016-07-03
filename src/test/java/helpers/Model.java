package helpers;

import java.math.BigDecimal;
import java.time.Instant;


public class Model {

  public interface Entity<T> {
    T getId();
    void setId(T id);
    String getType();
  }


  // ===============================================//

  public static class Bank implements Entity<Long> {

    private Long bic;
    private String name;

    public static String TYPE = "Bank";

    public Bank() {}

    public Bank(Long bic) {
      this.bic = bic;
    }

    @Override
    public Long getId() {
      return bic;
    }

    @Override
    public String getType() {
      return TYPE;
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public void setId(Long id) {
      this.bic = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }
  
  
  // -------- Bank subclasses ----------- //
  
  public static class LocalBank extends Bank {}
  
  public static class ForeignBank extends Bank {}

  
  // ===============================================//

  public static class Payment implements Entity<Long> {

    public static String TYPE = "Payment";

    private Long id;
    private BigDecimal amount;
    private Subject payer;
    private Subject receiver;
    private Instant timestamp;

    public Payment() {}

    public Payment(Long id) {
      this.id = id;
    }

    @Override
    public Long getId() {
      return id;
    }

    @Override
    public String getType() {
      return TYPE;
    }

    @Override
    public String toString() {
      return String.format("Payment of amount %s, payer=%s, receiver=%s", amount, payer, receiver);
    }

    @Override
    public void setId(Long id) {
      this.id = id;
    }

    public BigDecimal getAmount() {
      return amount;
    }

    public void setAmount(BigDecimal amount) {
      this.amount = amount;
    }

    public Subject getPayer() {
      return payer;
    }

    public void setPayer(Subject payer) {
      this.payer = payer;
    }

    public Subject getReceiver() {
      return receiver;
    }

    public void setReceiver(Subject receiver) {
      this.receiver = receiver;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
      this.timestamp = timestamp;
    }
  }


  // ===============================================//

  public static class Subj implements Entity<String> {

    private String bicAccount;
    private String name;
    private Bank bank;

    public Subj() {}

    public Subj(String bicAccount) {
      this.bicAccount = bicAccount;
    }

    @Override
    public String getId() {
      return bicAccount;
    }

    @Override
    public void setId(String id) {
      this.bicAccount = id;
    }

    @Override
    public String getType() {
      return "Subj";
    }

    @Override
    public String toString() {
      return String.format("%s [%s:%s]", name, bank, bicAccount);
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Bank getBank() {
      return bank;
    }

    public void setBank(Bank bank) {
      this.bank = bank;
    }
  }


  public static class Subject extends Subj {

    public Subject() {}

    public Subject(String bicAccount) {
      super(bicAccount);
    }

    @Override
    public String getType() {
      return "Subject";
    }
  }

}
