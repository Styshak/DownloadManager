package downloadmanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Observable;

public class Download extends Observable implements Runnable {

    //Максимальный размер буфера загрузки
    private static final int MAX_BUFFER_SIZE = 1024;

    // Названия состояний
    public static final String[] STATUSES = {"Downloading", "Paused",
        "Complete", "Canceled", "Error"};

    //Коды состояний
    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELED = 3;
    public static final int ERROR = 4;

    private URL url; // URL адрес загрузки
    private int size; // размер загружаеммых данных в байтах
    private int downloaded; // количество загруженных байтов
    private int status; // текущее состояние процесса загрузки
    private double speedInKBps;
    private String path;
    
    public Download(URL url, String path) {
        this.url = url;
        this.path = path;
        size = -1;
        downloaded = 0;
        status = DOWNLOADING;
        speedInKBps = 0;
        // Начало процесса загрузки
        download();
    }

    // Получаем URL-адрес для данного процесса загрузки
    public String getURL() {
        return url.toString();
    }

    // Определяем размер загружаеммых данных  
    public int getSize() {
        return size;
    }

    public double getSpeed() {
        return speedInKBps;
    }
    
    public String getPath() {
        return path;
    }
    
    // Получаем информацию о ходе данного процесса загрузки
    public float getProgress() {
        return ((float) downloaded / size) * 100;
    }

    public int getDownloaded() {
        return downloaded;
    }

    // Получаем сведения о состоянии данного процесса загрузки
    public int getStatus() {
        return status;
    }

    // Приостанавливаем данный процесс загрузки
    public void pause() {
        status = PAUSED;
        stateChanged();
    }

    // Возобновляем данный процесс загрузки
    public void resume() {
        status = DOWNLOADING;
        stateChanged();
        download();
    }

    // Отменяем данный процесс загрузки
    public void cancel() {
        status = CANCELED;
        stateChanged();
    }

    // В процесса загрузки возникла ошибка
    private void error() {
        status = ERROR;
        stateChanged();
    }

    //Начинаем или возобновляем процесс загрузки
    private void download() {
        Thread t = new Thread(this);
        t.start();
    }

    //Извлекаем имя файла из URL-адреса
    public String getFileName() {
        String newFileName = "File";
        String fileName = getFileNameFromURL();
        try {
            newFileName = URLDecoder.decode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return newFileName;
        }
        return newFileName;
    }

    private String getFileNameFromURL() {
        String fileName = url.getFile();
        return fileName.substring(fileName.lastIndexOf('/') + 1);
    }

    public static String encodeStr(String urlString) throws UnsupportedEncodingException {
        try {
            URI uri = new URI(urlString);
            return uri.toASCIIString();
        } catch (URISyntaxException e) {
            
        }
        return "";
    }

    // Загружаем файл
    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        RandomAccessFile file = null;
        InputStream stream = null;

        try {
            // Открываем соединение с данным URL-адресом
            HttpURLConnection connection
                    = (HttpURLConnection) url.openConnection();
            //Определяем, какую часть файла нужно загрузить
            connection.setRequestProperty("range", "bytes=" + downloaded + "-");
            // Соединяемся с сервером
            connection.connect();
            // Убеждаемся в том, что код отклика находится в диапазоне 200
            if (connection.getResponseCode() / 100 != 2) {
                error();
            }
            // Проверяем, имеет ли содержимое допустимую длину
            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                error();
            }
            // Задаем размер для данного процесса загрузки
            if (size == -1) {
                size = contentLength;
                stateChanged();
            }
            // Открываем файл и ищем конец файла
            file = new RandomAccessFile(path + getFileName(), "rw");
            file.seek(downloaded);
            stream = connection.getInputStream();
            
            while (status == DOWNLOADING) {
                // Задаем размер буфера так, чтобы загрузить оставшуюся часть файла
                byte buffer[];
                if (size - downloaded > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];
                } else {
                    buffer = new byte[size - downloaded];
                }
                // Производим чтение из сервера в буфер
                int read = stream.read(buffer);
                if (read == -1) {
                    break;
                }
                // Записываем содержимое буфера в файл
                file.write(buffer, 0, read);
                downloaded += read;
                try {
                    long timeInSecs = (System.currentTimeMillis() - startTime) / 1000; // Преобразовываем в секунды
                    speedInKBps = (downloaded / timeInSecs);
                } catch (ArithmeticException ae) {

                }
                stateChanged();
            }
            // определяем состояние как завершенное
            if (status == DOWNLOADING) {
                status = COMPLETE;
                stateChanged();
            }
        } catch (Exception e) {
            error();
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException ex) {
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    // Уведомляем наблюдателей об изменении состояния данного
    // процесса загрузки
    private void stateChanged() {
        setChanged();
        notifyObservers();
    }
}
