/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package downloadmanager;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author Sergey
 */
public class DownloadManager extends JFrame implements Observer {

    private JTextField addTextField;
    private DownloadsTableModel tableModel;
    private JTable table;
    private JButton pauseButton, resumeButton, cancelButton, clearButton;

    // Выделенный на данный момент процесс загрузки
    private Download selectedDownload;

    // Флаг, обозначающий, очищается или нет выбор таблицы
    private boolean clearing;

    public DownloadManager() {

        setTitle("Download Manager");
        setSize(840, 600);
        setResizable(false);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                actionExit();
            }
        });
        // Настройка меню File 
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem fileExitMenuItem = new JMenuItem("Exit", KeyEvent.VK_E);
        fileExitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionExit();
            }
        });
        fileMenu.add(fileExitMenuItem);

        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic(KeyEvent.VK_O);
        JMenuItem optionsFilePathItem = new JMenuItem("Change download folder", KeyEvent.VK_P);
        optionsFilePathItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionFilePath();
            }
        });
        optionsMenu.add(optionsFilePathItem);

        menuBar.add(fileMenu);
        menuBar.add(optionsMenu);
        setJMenuBar(menuBar);

        // Настройка панели добавления
        JPanel addPanel = new JPanel();
        addTextField = new JTextField(30);
        addPanel.add(addTextField);
        JButton addButton = new JButton("Add download");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionAdd();
            }
        });
        addPanel.add(addButton);

        // Найстройка таблицы 
        tableModel = new DownloadsTableModel();
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                tableSelectionChanged();
            }
        });
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    actionOpenFileLocation();
                }
            }
        });

        // настройка ProgressBar в качестве визуализатора для столбца
        // хода выполнения
        ProgressRender renderer = new ProgressRender();
        renderer.setStringPainted(true);
        table.setDefaultRenderer(JProgressBar.class, renderer);

        // Определение высоты строки таблицы, достаточной для того,
        // чтобы уместить JProgressBar
        table.setRowHeight((int) renderer.getPreferredSize().getHeight());

        table.getColumnModel().getColumn(0).setPreferredWidth(380);

        JPanel downloadsPanel = new JPanel();
        downloadsPanel.setBorder(BorderFactory.createTitledBorder("Downloads"));
        JScrollPane scrollPanel = new JScrollPane(table);
        scrollPanel.setPreferredSize(new Dimension(800, 430));
        downloadsPanel.add(scrollPanel, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel();
        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionPause();
            }
        });
        pauseButton.setEnabled(false);
        buttonsPanel.add(pauseButton);

        resumeButton = new JButton("Resume");
        resumeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionResume();
            }
        });
        resumeButton.setEnabled(false);
        buttonsPanel.add(resumeButton);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionCancel();
            }
        });
        cancelButton.setEnabled(false);
        buttonsPanel.add(cancelButton);

        clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionClear();
            }
        });
        clearButton.setEnabled(false);
        buttonsPanel.add(clearButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(addPanel, BorderLayout.NORTH);
        getContentPane().add(downloadsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }

    private void actionExit() {
        System.exit(0);
    }

    private void actionAdd() {
        URL verifiedUrl = verifyUrl(addTextField.getText());
        if (verifiedUrl != null) {
            tableModel.addDownload(new Download(verifiedUrl, FilePath.PATH));
            addTextField.setText("");
        } else {
            JOptionPane.showMessageDialog(this,
                    "Invalid Download URL", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private URL verifyUrl(String url) {
        if (!url.toLowerCase().startsWith("http://")) {
            return null;
        }
        URL verifiedUrl = null;
        try {
            verifiedUrl = new URL(url);
        } catch (MalformedURLException ex) {
            return null;
        }
        if (verifiedUrl.getFile().length() < 2) {
            return null;
        }
        return verifiedUrl;
    }

    private void tableSelectionChanged() {
        if (selectedDownload != null) {
            selectedDownload.deleteObserver(DownloadManager.this);
        }
        if (!clearing && table.getSelectedRow() > -1) {
            selectedDownload = tableModel.getDownload(table.getSelectedRow());
            selectedDownload.addObserver(DownloadManager.this);
            updateButtons();
        }
    }

    private void actionOpenFileLocation() {
        if (selectedDownload != null) {
            try {
                String path = selectedDownload.getPath()
                        + selectedDownload.getFileName();
                Runtime.getRuntime().exec("explorer.exe /select," + path);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "File not found!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void actionPause() {
        selectedDownload.pause();
        updateButtons();
    }

    private void actionResume() {
        selectedDownload.resume();
        updateButtons();
    }

    private void actionCancel() {
        selectedDownload.cancel();
        updateButtons();
    }

    private void actionClear() {
        clearing = true;
        tableModel.clearDownLoad(table.getSelectedRow());
        clearing = false;
        selectedDownload = null;
        updateButtons();
    }

    private void updateButtons() {
        if (selectedDownload != null) {
            int status = selectedDownload.getStatus();
            switch (status) {
                case Download.DOWNLOADING:
                    pauseButton.setEnabled(true);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(true);
                    clearButton.setEnabled(false);
                    break;
                case Download.PAUSED:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    clearButton.setEnabled(false);
                    break;
                case Download.ERROR:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    clearButton.setEnabled(true);
                    break;
                default:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    clearButton.setEnabled(true);
            }
        } else {
            pauseButton.setEnabled(false);
            resumeButton.setEnabled(false);
            cancelButton.setEnabled(false);
            clearButton.setEnabled(false);
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (selectedDownload != null && selectedDownload.equals(o)) {
            updateButtons();
        }
    }

    private void actionFilePath() {
        FilePath fp = new FilePath(this);
        fp.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DownloadManager manager = new DownloadManager();
            manager.setVisible(true);
        });
    }
}
