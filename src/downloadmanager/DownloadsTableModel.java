package downloadmanager;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JProgressBar;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Sergey
 */
public class DownloadsTableModel extends AbstractTableModel implements Observer {

    // Имена столбцов таблицы
    private static final String[] columnNames = {"File name", "Size", "Downloaded", "Progress",
        "Speed", "Status"};

    // Классы для значение каждого столбца
    private static final Class[] columnClasses = {String.class, String.class,
        String.class, JProgressBar.class, String.class, String.class};

    // Список процессов загрузки
    private final List<Download> downloadList = new ArrayList<>();

    // Добавление нового процесса загрузки в таблицу
    public void addDownload(Download download) {
        download.addObserver(this);
        downloadList.add(download);
        // Генерация для таблицы уведомления о вставке строки
        fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
    }

    // получение процесса загрузки для определенной строки
    public Download getDownload(int row) {
        return downloadList.get(row);
    }

    // Удаление процесса загрузки из списка
    public void clearDownLoad(int row) {
        downloadList.remove(row);
        // генерация для таблицы уведомление об удалении
        fireTableRowsDeleted(row, row);
    }

    private static String floatForm(double d) {
        return new DecimalFormat("#.##").format(d);
    }

    private static String bytesToHuman(int size) {
        int Kb = 1 * 1024;
        int Mb = Kb * 1024;
        int Gb = Mb * 1024;
        int Tb = Gb * 1024;

        if (size < Kb) {
            return floatForm(size) + " byte";
        }
        if (size >= Kb && size < Mb) {
            return floatForm((double) size / Kb) + " Kb";
        }
        if (size >= Mb && size < Gb) {
            return floatForm((double) size / Mb) + " Mb";
        }
        if (size >= Gb && size < Tb) {
            return floatForm((double) size / Gb) + " Gb";
        }
        return "???";
    }
    
    private String speedToHuman(double speed) {
        int Kb = 1 * 1024;
        int Mb = Kb * 1024;
        int Gb = Mb * 1024;
        
        if (speed < Kb) {
            return floatForm(speed) + " B/sec";
        }
        if (speed >= Kb && speed < Mb) {
            return floatForm((double) speed / Kb) + " KB/sec";
        }
        if (speed >= Mb && speed < Gb) {
            return floatForm((double) speed / Mb) + " MB/sec";
        }
        return "???";
    }

    @Override
    public Class getColumnClass(int col) {
        return columnClasses[col];
    }

    // Получение имени столбца
    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public int getRowCount() {
        return downloadList.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Download download = downloadList.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return download.getFileName();
            case 1:
                int size = download.getSize();
                return (size == -1) ? "" : bytesToHuman(size);
            case 2:
                int downloaded = download.getDownloaded();
                return (downloaded == 0) ? "" : bytesToHuman(downloaded);
            case 3:
                return download.getProgress();
            case 4:
                return speedToHuman(download.getSpeed());
            case 5:
                return Download.STATUSES[download.getStatus()];
        }
        return "";
    }

    @Override
    public void update(Observable o, Object arg) {
        int index = downloadList.indexOf(o);
        fireTableRowsUpdated(index, index);
    }
}
