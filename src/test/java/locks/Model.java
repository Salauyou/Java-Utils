package locks;

import java.math.BigDecimal;
import java.time.Instant;

public class Model {

    public interface Entity<T> {

        public T getId();
        
        public String getType();
        
    }
    
    //===============================================//
    
    public static class Bank implements Entity<Long> {

        private final Long bic;
        private String name;
        
        public static String TYPE = "Bank";
        
        
        /** public constructor **/
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
        
        
        public String getName() {
            return name;
        }

        public Bank setName(String name) {
            this.name = name;
            return this;
        }
    }
    
    //===============================================//
    
    public static class Payment implements Entity<Long> {

        public static String TYPE = "Payment";
        
        private final Long id;
        private BigDecimal amount;
        private Subject payer;
        private Subject receiver;
        private Instant timeStamp;

        
        /** public constructor **/
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
        
        

        public BigDecimal getAmount() {
            return amount;
        }

        public Payment setAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Subject getPayer() {
            return payer;
        }

        public Payment setPayer(Subject payer) {
            this.payer = payer;
            return this;
        }

        public Subject getReceiver() {
            return receiver;
        }

        public Payment setReceiver(Subject receiver) {
            this.receiver = receiver;
            return this;
        }

        public Instant getTimeStamp() {
            return timeStamp;
        }

        public Payment setTimeStamp(Instant timeStamp) {
            this.timeStamp = timeStamp;
            return this;
        }

    }
    
    //===============================================//
    
    public static class Subject implements Entity<String> {

        private final String bicAccount;
        private String name;
        private Bank bank;
        
        public static String TYPE = "Subject";
        
        /** public constructor **/
        public Subject(String bicAccount) {
            this.bicAccount = bicAccount;
        }
        
        
        @Override
        public String getId() {
            return bicAccount;
        }

        @Override
        public String getType() {
            return TYPE;
        }

        
        @Override
        public String toString() {
            return String.format("%s [%s:%s]", name, bank, bicAccount); 
        }
        
        
        public String getName() {
            return name;
        }


        public Subject setName(String name) {
            this.name = name;
            return this;
        }


        public Bank getBank() {
            return bank;
        }


        public Subject setBank(Bank bank) {
            this.bank = bank;
            return this;
        }

    }
    
}
