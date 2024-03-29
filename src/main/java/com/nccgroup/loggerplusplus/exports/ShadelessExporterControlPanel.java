package com.nccgroup.loggerplusplus.exports;

import com.coreyd97.BurpExtenderUtilities.Alignment;
import com.coreyd97.BurpExtenderUtilities.PanelBuilder;
import com.nccgroup.loggerplusplus.LoggerPlusPlus;
import com.nccgroup.loggerplusplus.util.Globals;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class ShadelessExporterControlPanel extends JPanel {

    private final ShadelessExporter shadelessExporter;
    private static final String STARTING_TEXT = "Starting Shadeless Exporter...";
    private static final String STOPPING_TEXT = "Stopping Shadeless Exporter...";
    private static final String START_TEXT = "Start Shadeless Exporter";
    private static final String STOP_TEXT = "Stop Shadeless Exporter";

    Logger logger = LogManager.getLogger(this);

    public ShadelessExporterControlPanel(ShadelessExporter shadelessExporter) {
        this.shadelessExporter = shadelessExporter;
        this.setLayout(new BorderLayout());

        JButton showConfigDialogButton = new JButton(new AbstractAction("Configure Shadeless Exporter") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                new ShadelessExporterConfigDialog(LoggerPlusPlus.instance.getLoggerFrame(), shadelessExporter)
                        .setVisible(true);
            }
        });

        JButton pingApiButton = new JButton(new AbstractAction("Ping Shadeless Api") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                URL urlHealthCheckObj;
                try {
                    urlHealthCheckObj = new URL(shadelessExporter.preferences.getSetting(Globals.PREF_SHADELESS_URL) + "/api/healthcheck");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Shadeless API URL is wrong format: " + e.getMessage() ,"Shadeless Exporter", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                HttpURLConnection con = null;
                try {
                    con = (HttpURLConnection) urlHealthCheckObj.openConnection();
                    con.setRequestMethod("GET");
                    con.setConnectTimeout(1000);
                    int responseCode = 0;
                    responseCode = con.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        JOptionPane.showMessageDialog(null, "Reached Shadeless API successfully" ,"Shadeless Exporter", JOptionPane.PLAIN_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(null, "Something is wrong???" ,"Shadeless Exporter", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Shadeless API is unreachable: " + e.getMessage() ,"Shadeless Exporter", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JToggleButton exportButton = new JToggleButton("Start Shadeless Exporter");
        exportButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                boolean buttonNowActive = exportButton.isSelected();
                exportButton.setEnabled(false);
                exportButton.setText(buttonNowActive ? STARTING_TEXT : STOPPING_TEXT);
                new SwingWorker<Boolean, Void>(){
                    Exception exception;

                    @Override
                    protected Boolean doInBackground() throws Exception {
                        boolean success = false;
                        try {
                            if (exportButton.isSelected()) {
                                enableExporter();
                            } else {
                                disableExporter();
                            }
                            success = true;
                        }catch (Exception e){
                            this.exception = e;
                        }
                        return success;
                    }

                    @Override
                    protected void done() {
                        try {
                            if(exception != null) {
                                JOptionPane.showMessageDialog(exportButton, "Could not start shadeless exporter: " +
                                        exception.getMessage() + "\nSee the logs for more information.", "Shadeless Exporter", JOptionPane.ERROR_MESSAGE);
                                logger.error("Could not start elastic exporter.", exception);
                            }
                            Boolean success = get();
                            boolean isRunning = buttonNowActive ^ !success;
                            exportButton.setSelected(isRunning);
                            showConfigDialogButton.setEnabled(!isRunning);

                            exportButton.setText(isRunning ? STOP_TEXT : START_TEXT);

                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                        exportButton.setEnabled(true);
                    }
                }.execute();
            }
        });

        if (isExporterEnabled()){
            exportButton.setSelected(true);
            exportButton.setText(STOP_TEXT);
            showConfigDialogButton.setEnabled(false);
        }


        this.add(PanelBuilder.build(new JComponent[][]{
                new JComponent[]{showConfigDialogButton},
                new JComponent[]{pingApiButton},
                new JComponent[]{exportButton}
        }, new int[][]{
                new int[]{1},
                new int[]{1}
        }, Alignment.FILL, 1.0, 1.0), BorderLayout.CENTER);


        this.setBorder(BorderFactory.createTitledBorder("Shadeless Exporter"));
    }

    private void enableExporter() throws Exception {
        this.shadelessExporter.getExportController().enableExporter(this.shadelessExporter);
    }

    private void disableExporter() throws Exception {
        this.shadelessExporter.getExportController().disableExporter(this.shadelessExporter);
    }

    private boolean isExporterEnabled() {
        return this.shadelessExporter.getExportController().getEnabledExporters().contains(this.shadelessExporter);
    }

}
