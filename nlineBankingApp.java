import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Simple Console-Based Online Banking System (Java + JDBC + MySQL)
 * Change DB_URL, DB_USER, DB_PASSWORD as per your system.
 */
public class OnlineBankingApp {

    // ----------------- DB UTIL -----------------
    static class DBUtil {
        private static final String DB_URL = "jdbc:mysql://localhost:3306/online_banking";
        private static final String DB_USER = "root";        // change
        private static final String DB_PASSWORD = "password"; // change

        public static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }
    }

    // ----------------- MODEL CLASSES -----------------
    static class Account {
        private String accountNumber;
        private int customerId;
        private String accountType;
        private double balance;

        public Account(String accountNumber, int customerId, String accountType, double balance) {
            this.accountNumber = accountNumber;
            this.customerId = customerId;
            this.accountType = accountType;
            this.balance = balance;
        }

        public String getAccountNumber() { return accountNumber; }
        public int getCustomerId() { return customerId; }
        public String getAccountType() { return accountType; }
        public double getBalance() { return balance; }
        public void setBalance(double balance) { this.balance = balance; }

        @Override
        public String toString() {
            return "Account{" +
                    "accountNumber='" + accountNumber + '\'' +
                    ", accountType='" + accountType + '\'' +
                    ", balance=" + balance +
                    '}';
        }
    }

    static class TransactionRecord {
        private int transactionId;
        private String accountNumber;
        private LocalDateTime dateTime;
        private String type;
        private double amount;
        private String description;
        private double closingBalance;

        public TransactionRecord(int transactionId, String accountNumber, LocalDateTime dateTime,
                                 String type, double amount, String description, double closingBalance) {
            this.transactionId = transactionId;
            this.accountNumber = accountNumber;
            this.dateTime = dateTime;
            this.type = type;
            this.amount = amount;
            this.description = description;
            this.closingBalance = closingBalance;
        }

        @Override
        public String toString() {
            return "[" + transactionId + "] " + dateTime +
                    " | " + type +
                    " | " + amount +
                    " | " + description +
                    " | Balance: " + closingBalance;
        }
    }

    // ----------------- DAO (DATABASE OPERATIONS) -----------------
    static class BankingDAO {

        public Account login(String accountNumber, String pin) {
            String sql = "SELECT * FROM accounts WHERE account_number = ? AND pin = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, accountNumber);
                ps.setString(2, pin);

                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int customerId = rs.getInt("customer_id");
                    String accountType = rs.getString("account_type");
                    double balance = rs.getDouble("balance");
                    return new Account(accountNumber, customerId, accountType, balance);
                }
            } catch (SQLException e) {
                System.out.println("Error during login: " + e.getMessage());
            }
            return null;
        }

        public double getBalance(String accountNumber) {
            String sql = "SELECT balance FROM accounts WHERE account_number = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, accountNumber);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            } catch (SQLException e) {
                System.out.println("Error fetching balance: " + e.getMessage());
            }
            return -1;
        }

        public boolean accountExists(String accountNumber) {
            String sql = "SELECT account_number FROM accounts WHERE account_number = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, accountNumber);
                ResultSet rs = ps.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                System.out.println("Error checking account: " + e.getMessage());
            }
            return false;
        }

        public boolean updateBalance(String accountNumber, double newBalance) {
            String sql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setDouble(1, newBalance);
                ps.setString(2, accountNumber);

                int rows = ps.executeUpdate();
                return rows > 0;
            } catch (SQLException e) {
                System.out.println("Error updating balance: " + e.getMessage());
            }
            return false;
        }

        public boolean addTransaction(String accountNumber, String type, double amount,
                                      String description, double closingBalance) {
            String sql = "INSERT INTO transactions " +
                    "(account_number, date_time, type, amount, description, closing_balance) " +
                    "VALUES (?, NOW(), ?, ?, ?, ?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, accountNumber);
                ps.setString(2, type);
                ps.setDouble(3, amount);
                ps.setString(4, description);
                ps.setDouble(5, closingBalance);

                int rows = ps.executeUpdate();
                return rows > 0;
            } catch (SQLException e) {
                System.out.println("Error adding transaction: " + e.getMessage());
            }
            return false;
        }

        public List<TransactionRecord> getTransactions(String accountNumber) {
            List<TransactionRecord> list = new ArrayList<>();
            String sql = "SELECT * FROM transactions WHERE account_number = ? ORDER BY date_time DESC";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, accountNumber);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("transaction_id");
                    Timestamp ts = rs.getTimestamp("date_time");
                    String type = rs.getString("type");
                    double amount = rs.getDouble("amount");
                    String description = rs.getString("description");
                    double closingBalance = rs.getDouble("closing_balance");

                    TransactionRecord tr = new TransactionRecord(
                            id, accountNumber,
                            ts.toLocalDateTime(), type, amount, description, closingBalance
                    );
                    list.add(tr);
                }
            } catch (SQLException e) {
                System.out.println("Error fetching transactions: " + e.getMessage());
            }
            return list;
        }
    }

    // ----------------- SERVICE (BUSINESS LOGIC) -----------------
    static class BankingService {
        private BankingDAO dao = new BankingDAO();

        public Account login(String accountNumber, String pin) {
            return dao.login(accountNumber, pin);
        }

        public double checkBalance(String accountNumber) {
            return dao.getBalance(accountNumber);
        }

        public boolean transferFunds(String fromAcc, String toAcc, double amount) {
            if (amount <= 0) {
                System.out.println("Amount must be greater than 0.");
                return false;
            }

            if (!dao.accountExists(toAcc)) {
                System.out.println("Destination account does not exist.");
                return false;
            }

            double fromBalance = dao.getBalance(fromAcc);
            if (fromBalance < amount) {
                System.out.println("Insufficient balance.");
                return false;
            }

            // We are not handling transactions (commit/rollback) here for simplicity
            double newFromBalance = fromBalance - amount;
            boolean debited = dao.updateBalance(fromAcc, newFromBalance);
            if (!debited) {
                System.out.println("Error debiting sender account.");
                return false;
            }

            double toBalance = dao.getBalance(toAcc);
            double newToBalance = toBalance + amount;
            boolean credited = dao.updateBalance(toAcc, newToBalance);
            if (!credited) {
                System.out.println("Error crediting receiver account.");
                // Ideally we should rollback the previous update
                return false;
            }

            dao.addTransaction(fromAcc, "Debit", amount,
                    "Transfer to " + toAcc, newFromBalance);
            dao.addTransaction(toAcc, "Credit", amount,
                    "Transfer from " + fromAcc, newToBalance);

            return true;
        }

        public List<TransactionRecord> getMiniStatement(String accountNumber) {
            return dao.getTransactions(accountNumber);
        }
    }

    // ----------------- MAIN (UI) -----------------
    public static void main(String[] args) {

        // Load driver (for older MySQL drivers; newer sometimes auto-load)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC driver not found.");
            return;
        }

        BankingService service = new BankingService();
        Scanner sc = new Scanner(System.in);

        System.out.println("===== ONLINE BANKING SYSTEM =====");
        System.out.print("Enter Account Number: ");
        String accNo = sc.nextLine();

        System.out.print("Enter PIN: ");
        String pin = sc.nextLine();

        Account loggedIn = service.login(accNo, pin);

        if (loggedIn == null) {
            System.out.println("Invalid account number or PIN.");
            return;
        }

        System.out.println("Welcome! Account: " + loggedIn.getAccountNumber() +
                " | Type: " + loggedIn.getAccountType());
        int choice;

        do {
            System.out.println("\n----- MENU -----");
            System.out.println("1. Check Balance");
            System.out.println("2. Transfer Funds");
            System.out.println("3. Mini Statement (Transactions)");
            System.out.println("4. Exit");
            System.out.print("Enter your choice: ");
            choice = Integer.parseInt(sc.nextLine());

            switch (choice) {
                case 1:
                    double bal = service.checkBalance(loggedIn.getAccountNumber());
                    System.out.println("Current Balance: " + bal);
                    break;

                case 2:
                    System.out.print("Enter destination account number: ");
                    String toAcc = sc.nextLine();
                    System.out.print("Enter amount to transfer: ");
                    double amount = Double.parseDouble(sc.nextLine());

                    boolean success = service.transferFunds(
                            loggedIn.getAccountNumber(), toAcc, amount);

                    if (success) {
                        System.out.println("Transfer successful!");
                    } else {
                        System.out.println("Transfer failed.");
                    }
                    break;

                case 3:
                    List<TransactionRecord> list =
                            service.getMiniStatement(loggedIn.getAccountNumber());
                    if (list.isEmpty()) {
                        System.out.println("No transactions found.");
                    } else {
                        System.out.println("----- MINI STATEMENT -----");
                        for (TransactionRecord tr : list) {
                            System.out.println(tr);
                        }
                    }
                    break;

                case 4:
                    System.out.println("Thank you for using Online Banking.");
                    break;

                default:
                    System.out.println("Invalid choice. Try again.");
            }
        } while (choice != 4);

        sc.close();
    }
}
