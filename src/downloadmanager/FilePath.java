/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package downloadmanager;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author Sergey
 */
public class FilePath extends JFrame {

    private JTextField jTextPath;
    private JButton chooseButton;
    public static String PATH = "B:\\";
    private DownloadManager downloadManager;
    
    public FilePath(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
        this.downloadManager.setVisible(false);
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Choose download files path");
        setSize(440, 80);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel fcPanel = new JPanel();
        jTextPath = new JTextField(30);
        jTextPath.setEditable(false);
        jTextPath.setText(PATH);
        fcPanel.add(jTextPath);
        
        chooseButton = new JButton("Browse...");
        chooseButton.addActionListener((ActionEvent e) -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(PATH));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = chooser.showDialog(FilePath.this, "Select");
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                PATH = chooser.getSelectedFile().toString() + "\\";
                jTextPath.setText(PATH);
            }
        });
        
        fcPanel.add(chooseButton);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(fcPanel, BorderLayout.NORTH);
    }
    
    @Override
    public void dispose() {
        downloadManager.setVisible(true);
        this.setVisible(false);
    }
}
