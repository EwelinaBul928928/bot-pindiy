import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class main {


    private static class SerializableCookie implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String name;
        private String value;
        private String domain;
        private String path;
        private Date expiry;
        private boolean isSecure;
        private boolean isHttpOnly;
        
        public SerializableCookie(Cookie cookie) {
            this.name = cookie.getName();
            this.value = cookie.getValue();
            this.domain = cookie.getDomain();
            this.path = cookie.getPath();
            this.expiry = cookie.getExpiry();
            this.isSecure = cookie.isSecure();
            this.isHttpOnly = cookie.isHttpOnly();
        }
        
        public Cookie toSeleniumCookie() {
            return new Cookie.Builder(name, value)
                .domain(domain)
                .path(path)
                .expiresOn(expiry)
                .isSecure(isSecure)
                .isHttpOnly(isHttpOnly)
                .build();
        }
        

        public String getName() { return name; }
        public String getValue() { return value; }
        public String getDomain() { return domain; }
        public String getPath() { return path; }
        public Date getExpiry() { return expiry; }
        public boolean isSecure() { return isSecure; }
        public boolean isHttpOnly() { return isHttpOnly; }
    }

    private final List<String> comments = List.of(
        " thanks for sharing"
    );

    private final Random rand = new Random();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Properties config = new Properties();
    private WebDriver driver; 
    private boolean isLoggedIn = false; 
    private int commentCount = 0; 
    private int currentPage = 1; 
    private Set<String> commentedThreads = new HashSet<>(); 


    private String username;
    private String password;
    private String threadUrl;
    private String threadsListUrl;
    private String chromeDriverPath;
    private int minDelay;
    private int maxDelay;
    private boolean headlessMode;
    private boolean maximizeWindow;
    private boolean saveCookies;
    private boolean loadCookies;
    private String cookiesFilePath;
    private int sessionCheckInterval; 

    public static void main(String[] args) {
        try {
            new main().start();
        } catch (Exception e) {
            System.err.println("Błąd podczas uruchamiania bota: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadConfig() throws IOException {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            config.load(fis);
            
            username = config.getProperty("username");
            password = config.getProperty("password");
            threadUrl = config.getProperty("thread_url");
            threadsListUrl = config.getProperty("threads_list_url"); 
            chromeDriverPath = config.getProperty("chrome_driver_path");
            minDelay = Integer.parseInt(config.getProperty("min_delay", "1"));
            maxDelay = Integer.parseInt(config.getProperty("max_delay", "1"));
            headlessMode = Boolean.parseBoolean(config.getProperty("headless_mode", "false"));
            maximizeWindow = Boolean.parseBoolean(config.getProperty("maximize_window", "true"));
            saveCookies = Boolean.parseBoolean(config.getProperty("save_cookies", "true"));
            loadCookies = Boolean.parseBoolean(config.getProperty("load_cookies", "true"));
            cookiesFilePath = config.getProperty("cookies_file_path", "cookies.dat");
            sessionCheckInterval = Integer.parseInt(config.getProperty("session_check_interval", "5"));
            
            System.out.println("Konfiguracja załadowana pomyślnie");
        }
    }

    private void start() throws IOException {
        System.out.println("Bot Started");
        loadConfig();
        
        System.out.println("Logowanie się");
        loginToPindiy();
        
        if (isLoggedIn) {
            System.out.println(" Zalogowano pomyślnie");
            scheduleNext();
        } else {
            System.err.println(" Nie udało się zalogować");
        }
    }

    private void loginToPindiy() {
        try {
            System.setProperty("webdriver.chrome.driver", chromeDriverPath);
            ChromeOptions options = new ChromeOptions();
            if (maximizeWindow) {
                options.addArguments("--start-maximized");
            }
            if (headlessMode) {
                options.addArguments("--headless");
            }
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-popup-blocking");
            options.addArguments("--disable-extensions");
            options.addArguments("--no-first-run");

            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            
            handleCookieConsent();
            
            boolean cookiesLoaded = false;
            if (loadCookies) {
                cookiesLoaded = loadCookiesFromFile();
            }
            
    
            if (cookiesLoaded) {
                if (validateCookies()) {
                    isLoggedIn = true;
                    System.out.println("");
                    return;
                } else {
                    System.out.println("");
                }
            }
            
            System.out.println("Otwieram stronę logowania...");
            driver.get("https://www.pindiy.com/member.php?mod=logging&action=login");
            Thread.sleep(3000);
            
            handleCookieConsent();
            
            System.out.println("Strona załadowana");

            WebElement userField = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username")));
            WebElement passField = driver.findElement(By.name("password"));
            
            System.out.println("logowanie");
            userField.sendKeys(username);
            passField.sendKeys(password);

            WebElement loginBtn = findLoginButton(wait);
            System.out.println("Przycisk logowania znaleziony. Klikam...");
            loginBtn.click();


            System.out.println("Czekam");
            Thread.sleep(5000); 
            
            // Sprawdź czy logowanie się powiodło
            String currentUrl = driver.getCurrentUrl();
            System.out.println("URL po logowaniu: " + currentUrl);
            
            if (currentUrl.contains("pindiy.com") && !currentUrl.contains("logging")) {
                isLoggedIn = true;
                
                try {
                    driver.findElement(By.xpath("//a[contains(@href, 'space-uid')]"));
                    System.out.println("Potwierdzono zalogowanie");
                } catch (Exception e) {
                    System.out.println("Nie znaleziono elementu");
                }
                
                checkAndSaveCookies();
            } else {
                System.err.println("Logowanie nie powiodło się");
                System.err.println("URL: " + currentUrl);
                isLoggedIn = false;
            }
            
        } catch (Exception e) {
            System.err.println("Błąd podczas logowania: " + e.getMessage());
            isLoggedIn = false;
        }
    }

    private boolean checkIfStillLoggedIn() {
        try {
            // Sprawdź czy jesteśmy na stronie wymagającej logowania
            String currentUrl = driver.getCurrentUrl();
            
            // Jeśli jesteśmy na stronie logowania, oznacza to że zostaliśmy wylogowani
            if (currentUrl.contains("logging") || currentUrl.contains("login")) {
                System.out.println("Wykryto wylogowanie");
                return false;
            }
            
            try {
                driver.findElement(By.xpath("//a[contains(@href, 'space-uid')]"));
                System.out.println("Sesja nadal aktywna");
                return true;
            } catch (Exception e) {
                System.out.println("prawdopodobnie wylogowani");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("Błąd podczas sprawdzania sesji:" + e.getMessage());
            return false;
        }
    }
    

    private boolean attemptReLogin() {
        try {
            System.out.println("Próbuję ponownie zalogować...");
            

            driver.get("https://www.pindiy.com/member.php?mod=logging&action=login");
            Thread.sleep(3000);
            

            handleCookieConsent();
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            
            WebElement userField = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username")));
            WebElement passField = driver.findElement(By.name("password"));
            
            userField.clear();
            passField.clear();
            userField.sendKeys(username);
            passField.sendKeys(password);
            
           
            WebElement loginBtn = findLoginButton(wait);
            loginBtn.click();
            
           
            Thread.sleep(5000);
            
        
            String currentUrl = driver.getCurrentUrl();
            if (currentUrl.contains("pindiy.com") && !currentUrl.contains("logging")) {
                isLoggedIn = true;
                System.out.println("Ponowne logowanie");
                

                if (saveCookies) {
                    Set<Cookie> cookies = driver.manage().getCookies();
                    saveCookiesToFile(cookies);
                }
                
                return true;
            } else {
                System.err.println("logowanie nie powiodło się");
                isLoggedIn = false;
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Błądlogowania: " + e.getMessage());
            isLoggedIn = false;
            return false;
        }
    }
    private boolean validateCookies() {
        try {
            System.out.println("Sprawdzamcookies");
            
            driver.get("https://www.pindiy.com/member.php?mod=logging&action=login");
            Thread.sleep(3000);
            
            String currentUrl = driver.getCurrentUrl();
            System.out.println("URL po sprawdzeniu cookies: " + currentUrl);
            
            if (!currentUrl.contains("logging")) {
                System.out.println("jesteśmy zalogowani");
                return true;
            } else {
                System.out.println("wymagane ponowne logowanie");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Błąd" + e.getMessage());
            return false;
        }
    }

    private boolean hasAlreadyCommented(String threadUrl) {
        try {
            System.out.println("Sprawdzam komentarz" + threadUrl);
            
            if (commentedThreads.contains(threadUrl)) {
                return true;
            }
            
            List<WebElement> existingComments = driver.findElements(By.xpath("//div[contains(@class, 'post')]//div[contains(text(), 'thanks for sharing')]"));
            
            if (!existingComments.isEmpty()) {
                commentedThreads.add(threadUrl);
                return true;
            }
            
            try {
                WebElement commentField = driver.findElement(By.id("fastpostmessage"));
                if (commentField != null) {
                    System.out.println("komentuje");
                    return false;
                }
            } catch (Exception e) {
                commentedThreads.add(threadUrl);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            System.out.println("Błąd podczas sprawdzania komentarzy: " + e.getMessage());
            return false; 
        }
    }
    
    private String selectUncommentedThread(List<WebElement> threadLinks) {
        List<WebElement> uncommentedThreads = new ArrayList<>();
        
        for (WebElement link : threadLinks) {
            String threadUrl = link.getAttribute("href");
            if (!commentedThreads.contains(threadUrl)) {
                uncommentedThreads.add(link);
            }
        }
        
        if (uncommentedThreads.isEmpty()) {
            commentedThreads.clear();
            return threadLinks.get(rand.nextInt(threadLinks.size())).getAttribute("href");
        }
        
        return uncommentedThreads.get(rand.nextInt(uncommentedThreads.size())).getAttribute("href");
    }
    
    private WebElement findLoginButton(WebDriverWait wait) throws Exception {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(By.className("logging")));
        } catch (Exception e1) {
            try {
                return wait.until(ExpectedConditions.elementToBeClickable(By.name("loginsubmit")));
            } catch (Exception e2) {
                try {
                    return wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='submit']")));
                } catch (Exception e3) {
                    return wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Login') or contains(text(),'Zaloguj')]")));
                }
            }
        }
    }
    
    private void scheduleNext() {
        int seconds = rand.nextInt(maxDelay - minDelay + 1) + minDelay;
        System.out.println("Next comment in " + seconds + " seconds.");
        scheduler.schedule(this::executeTask, seconds, TimeUnit.SECONDS);
    }

    private void executeTask() {
        if (commentCount % sessionCheckInterval == 0) {
            if (!checkIfStillLoggedIn()) {
                if (!attemptReLogin()) {
                    scheduleNext();
                    return;
                }
            }
        }
        
        String comment = comments.get(rand.nextInt(comments.size()));
        try {
            sendCommentOnly(comment);
        } catch (Exception e) {
            
            if (e.getMessage().contains("session") || e.getMessage().contains("invalid session") || 
                e.getMessage().contains("not connected") || e.getMessage().contains("disconnected")) {
                System.out.println("Błąd sesji");
                isLoggedIn = false;
                if (attemptReLogin()) {
                    System.out.println("Ponowne logowanie udane");
                }
            }
            
            if (!e.getMessage().contains("Już komentowałem") && !e.getMessage().contains("przechodzę do następnego")) {
                e.printStackTrace();
            }
        } finally {
            scheduleNext();
        }
    }

    private void sendCommentOnly(String comment) throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        try {
            
            System.out.println("Przechodzę do strony głównej: " + threadsListUrl);
            driver.get(threadsListUrl);
            Thread.sleep(3000);
            handleCookieConsent();

            List<WebElement> threadLinks = null;
            
            try {
                threadLinks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//a[contains(@href, 'thread-') and contains(@href, '-1-1.html')]")));
                System.out.println("Znaleziono linki z selektorem -1-1.html");
            } catch (Exception e1) {
                try {
                    threadLinks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//a[contains(@href, 'thread-')]")));
                    System.out.println("Znaleziono linki z uniwersalnym selektorem");
                } catch (Exception e2) {
                    try {
                        threadLinks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//div[contains(@class, 'forum')]//a[contains(@href, 'thread-')]")));
                        System.out.println("Znaleziono linki w sekcjach forum");
                    } catch (Exception e3) {
                        try {
                            threadLinks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//table//a[contains(@href, 'thread-')]")));
                            System.out.println("Znaleziono linki w tabelach");
                        } catch (Exception e4) {
                            System.err.println("Nie można znaleźć żadnych linków do wątków na stronie głównej!");
                            System.err.println("URL strony: " + driver.getCurrentUrl());
                            System.err.println("Tytuł strony: " + driver.getTitle());
                            throw new NoSuchElementException("Brak linków do wątków na stronie głównej");
                        }
                    }
                }
            }
            
            if (threadLinks == null || threadLinks.isEmpty()) {
                System.err.println("Nie znaleziono żadnych linków do wątków na stronie głównej");
                throw new NoSuchElementException("Brak linków do wątków.");
            }

            System.out.println("Znaleziono " + threadLinks.size() + " linków do wątków na stronie głównej");

            String selectedThreadUrl = selectUncommentedThread(threadLinks);
            System.out.println("Wybrano wątek: " + selectedThreadUrl);

            // Przejdź do wybranego wątku
            System.out.println("Przechodzę do wątku: " + selectedThreadUrl);
            driver.get(selectedThreadUrl);
            Thread.sleep(3000);
            
            String currentUrl = driver.getCurrentUrl();
            String pageTitle = driver.getTitle();
            System.out.println("Strona wątku załadowana - URL: " + currentUrl);
            System.out.println("Tytuł strony: " + pageTitle);
            
            Thread.sleep(2000);
            
            
            if (hasAlreadyCommented(selectedThreadUrl)) {
                commentedThreads.add(selectedThreadUrl);
                return;
            }

            System.out.println("Szukam pola komentarza...");
            WebElement msgBox = null;
            
            try {
                
                msgBox = wait.until(ExpectedConditions.elementToBeClickable(By.id("fastpostmessage")));
                System.out.println("Pole komentarza znalezione przez ID 'fastpostmessage'");
            } catch (Exception e1) {
                try {
                    msgBox = wait.until(ExpectedConditions.elementToBeClickable(By.name("message")));
                    System.out.println("Pole komentarza znalezione przez name 'message'");
                } catch (Exception e2) {
                    try {
                        msgBox = wait.until(ExpectedConditions.elementToBeClickable(By.tagName("textarea")));
                        System.out.println("Pole komentarza znalezione przez tag 'textarea'");
                    } catch (Exception e3) {
                        System.err.println("Nie można znaleźć pola komentarza!");
                        System.err.println("URL strony: " + driver.getCurrentUrl());
                        System.err.println("Tytuł strony: " + driver.getTitle());
                        throw new RuntimeException("Pole komentarza nie zostało znalezione");
                    }
                }
            }
            
            System.out.println("Pole komentarza znalezione i gotowe do użycia. Wprowadzam komentarz: " + comment);
            
            String currentValue = msgBox.getAttribute("value");
            if (currentValue != null && !currentValue.isEmpty()) {
                msgBox.clear();
                Thread.sleep(500);
            }
            
            msgBox.sendKeys(comment);
            Thread.sleep(1000);

            System.out.println("Szukam przycisku wyślij...");
            WebElement submitBtn = null;
            
            try {
                submitBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("fastpostsubmit")));
                System.out.println("Przycisk wyślij znaleziony przez ID 'fastpostsubmit'");
            } catch (Exception e1) {
                try {
                    submitBtn = wait.until(ExpectedConditions.elementToBeClickable(By.name("replysubmit")));
                    System.out.println("Przycisk wyślij znaleziony przez name 'replysubmit'");
                } catch (Exception e2) {
                    try {
                        submitBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='submit']")));
                        System.out.println("Przycisk wyślij znaleziony przez input submit");
                    } catch (Exception e3) {
                        try {
                            submitBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(), 'Submit') or contains(text(), 'Wyślij') or contains(text(), 'Reply')]")));
                            System.out.println("Przycisk wyślij znaleziony przez button");
                        } catch (Exception e4) {
                            System.err.println("Nie można znaleźć przycisku wyślij!");
                            System.err.println("URL strony: " + driver.getCurrentUrl());
                            System.err.println("Tytuł strony: " + driver.getTitle());
                            throw new RuntimeException("Przycisk wyślij nie został znaleziony");
                        }
                    }
                }
            }
            
            System.out.println("Przycisk wyślij znaleziony i gotowy. Klikam...");
            submitBtn.click();
            Thread.sleep(3000); 
            
            commentCount++;
            commentedThreads.add(selectedThreadUrl);
            System.out.println("Komentarz wysłany pomyślnie! (Komentarz #" + commentCount + ")");
            System.out.println("Dodano wątek do listy komentowanych: " + selectedThreadUrl);
            
        } catch (Exception e) {
            System.err.println("Błąd podczas wysyłania komentarza: " + e.getMessage());
            throw e;
        }
    }
}
