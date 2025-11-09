import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * WasteClassifierGUI
 * Stage1 + Stage2 combined:
 * - Classification + learning (persisted)
 * - Report view with search, delete selected, Export CSV, Export PDF(text)
 * - Analytics dashboard (Bar + Pie) implemented with pure Swing drawing
 *
 * No external libraries required.
 */
public class WasteClassifierGUI extends JFrame {

    private JTextField inputField, searchField;
    private JTextArea outputArea;
    private JButton classifyButton, clearButton, exitButton, reportButton, deleteButton;

    private Map<String, String> categoryMap = new HashMap<>();
    private Map<String, String> outcomeMap = new HashMap<>();

    private final String DATA_FILE = "waste_data.txt";
    private final String LEARNING_FILE = "learned_items.txt";

    public WasteClassifierGUI() {
        setTitle("Smart Waste Classifier");
        setSize(820, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(225, 235, 250));

        // --------- TOP PANEL (INPUT AREA) ----------
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(new Color(200, 220, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel label = new JLabel("Enter Waste Item:");
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(label, gbc);

        inputField = new JTextField(30); // made wider
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        gbc.gridx = 1;
        gbc.gridy = 0;
        topPanel.add(inputField, gbc);

        classifyButton = new JButton("Classify");
        styleButton(classifyButton, new Color(66, 133, 244));
        gbc.gridx = 2;
        gbc.gridy = 0;
        topPanel.add(classifyButton, gbc);

        add(topPanel, BorderLayout.NORTH);

        // --------- CENTER PANEL (OUTPUT AREA) ----------
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        outputArea.setBorder(BorderFactory.createTitledBorder("Classification Results"));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        // --------- BOTTOM PANEL ----------
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBackground(new Color(200, 220, 250));

        clearButton = new JButton("Clear");
        styleButton(clearButton, new Color(244, 180, 0));

        reportButton = new JButton("View Report");
        styleButton(reportButton, new Color(52, 168, 83));

        deleteButton = new JButton("Delete Entry");
        styleButton(deleteButton, new Color(219, 68, 55));

        exitButton = new JButton("Exit");
        styleButton(exitButton, new Color(100, 100, 100));

        bottomPanel.add(clearButton);
        bottomPanel.add(reportButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(exitButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // --------- LOAD CATEGORIES ----------
        loadKeywords();
        loadLearnedItems(); // load previously learned items into categoryMap

        // --------- ACTIONS ----------
        classifyButton.addActionListener(e -> classifyItem());
        clearButton.addActionListener(e -> outputArea.setText(""));
        exitButton.addActionListener(e -> System.exit(0));
        reportButton.addActionListener(e -> showReport());
        deleteButton.addActionListener(e -> deleteEntry()); // existing delete-by-name dialog

        // make Enter key classify
        inputField.addActionListener(e -> classifyItem());
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
    }

    private void loadKeywords() {
        // --- RECYCLABLE (25) ---
       String[] recyclable = {
    "plastic", "paper", "newspaper", "bottle", "glass", 
    "cardboard", "can", "metal", "tin", "jar",
    "aluminum", "magazine", "book", "carton", "foil",
    "cup", "steel", "tray", "wrapper", "container",
    "leaflet", "brochure", "envelope", "packaging", "poster",
    "notebook", "pamphlet", "cardboardbox", "soda can", "beer bottle",
    "wine bottle", "milk jug", "detergent bottle", "shampoo bottle", "cereal box",
    "egg carton", "pizza box", "food can", "soup can", "pet bottle",
    "plastic container", "metal lid", "glass jar", "paper bag", "cardboard tube",
    "office paper", "junk mail", "catalog", "phone book", "cardboard packaging"
};

        // --- E-WASTE (25) ---
        String[] ewaste = {
    "laptop", "tablet", "phone", "charger", "cable",
    "wire", "keyboard", "mouse", "monitor", "tv",
    "remote", "cpu", "motherboard", "battery", "speaker",
    "earphone", "printer", "router", "camera", "calculator",
    "smartwatch", "pendrive", "harddisk", "adapter", "cd",
    "dvd", "scanner", "projector", "modem", "headphones",
    "microphone", "webcam", "game console", "playstation", "xbox",
    "circuit board", "power supply", "server", "ram", "memory card",
    "external drive", "usb cable", "hdmi cable", "vcr", "dvd player",
    "bluray player", "home theater", "satellite receiver", "gps device", "digital camera"
};

        // --- HAZARDOUS (25) ---
        String[] hazardous = {
    "acid", "chemical", "pesticide", "paint", "thinner",
    "solvent", "bleach", "ammonia", "detergent", "disinfectant",
    "medicine", "drug", "injection", "syringe", "battery",
    "nailpolish", "perfume", "oil", "fuel", "petrol",
    "diesel", "mercury", "labwaste", "toxic", "arsenic",
    "herbicide", "insecticide", "fertilizer", "gasoline", "kerosene",
    "motor oil", "antifreeze", "brake fluid", "weed killer", "rat poison",
    "aerosol can", "propane tank", "butane", "lighter fluid", "pool chemicals",
    "hair dye", "acetone", "turpentine", "varnish", "stain",
    "wood preservative", "mothballs", "air freshener", "cleaner", "degreaser"
};

        // --- NON-RECYCLABLE (25) ---
     String[] nonRecycle = {
    "cloth", "shirt", "wood", "sponge", "rubber",
    "leather", "ceramic", "tile", "ash", "dust",
    "diaper", "sanitarypad", "tissue", "styrofoam", "thermocol",
    "cushion", "plasticbag", "chipbag", "mixedwaste", "synthetic",
    "rag", "footwear", "mirror", "fabric", "rope",
    "broken glass", "light bulb", "window glass", "pyrex", "ovenware",
    "medical waste", "bandage", "cotton swab", "cigarette butt", "gum",
    "wax paper", "parchment paper", "carbon paper", "photos", "sticker",
    "wallpaper", "paintbrush", "sponge", "toothbrush", "comb",
    "pet food bag", "chip wrapper", "candy wrapper", "juice box", "pouch"
};

        for (String w : recyclable) categoryMap.put(w, "Recyclable Waste");
        for (String w : ewaste) categoryMap.put(w, "E-Waste");
        for (String w : hazardous) categoryMap.put(w, "Hazardous Waste");
        for (String w : nonRecycle) categoryMap.put(w, "Non-Recyclable Waste");

        // Outcomes
        outcomeMap.put("Recyclable Waste", "Recyclable in industrial process");
        outcomeMap.put("E-Waste", "Recycle via electronic waste channels");
        outcomeMap.put("Hazardous Waste", "Handle using special disposal facilities");
        outcomeMap.put("Non-Recyclable Waste", "Can be composted or decomposed naturally");
        outcomeMap.put("Not Classified", "Item not recognized. Please categorize manually.");
    }

    private void loadLearnedItems() {
        File f = new File(LEARNING_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                // format: item:category
                String[] parts = line.split(":", 2);
                if (parts.length == 2) categoryMap.put(parts[0].trim().toLowerCase(), parts[1].trim());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void saveLearnedItem(String item, String category) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(LEARNING_FILE, true)))) {
            out.println(item + ":" + category);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void classifyItem() {
        String item = inputField.getText().trim().toLowerCase();
        if (item.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a waste item!");
            return;
        }

        String category = categoryMap.getOrDefault(item, "Not Classified");

        // If not known, ask user (Stage 1 learning)
        if (category.equals("Not Classified")) {
            String[] options = {"Recyclable Waste", "E-Waste", "Hazardous Waste", "Non-Recyclable Waste"};
            String chosen = (String) JOptionPane.showInputDialog(
                    this,
                    "Item not classified. Select category for '" + item + "':",
                    "Manual Classification",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );
            if (chosen != null) {
                category = chosen;
                categoryMap.put(item, chosen);    // add to in-memory map
                saveLearnedItem(item, chosen);    // persist for future runs
            } else {
                // user cancelled - do not save or add
                outputArea.append("Item: " + item + "\nSkipped classification.\n----------------------------------------\n");
                inputField.setText("");
                return;
            }
        }

        String outcome = outcomeMap.getOrDefault(category, "Not Classified");
        String result = "Item: " + item + "\nCategory: " + category + "\nOutcome: " + outcome +
                "\n----------------------------------------\n";
        outputArea.append(result);

        // store with timestamp (date & time saved in file; not printed in output area)
        saveToFile(item, category, outcome);
        inputField.setText("");
    }

    private void saveToFile(String item, String category, String outcome) {
        try (FileWriter fw = new FileWriter(DATA_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            String dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
            out.println("Item: " + item);
            out.println("Category: " + category);
            out.println("Outcome: " + outcome);
            out.println("Date & Time: " + dateTime);
            out.println("----------------------------------------");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // keeps your original delete-by-name dialog
    private void deleteEntry() {
        String itemToDelete = JOptionPane.showInputDialog(this, "Enter item name to delete:");
        if (itemToDelete == null || itemToDelete.trim().isEmpty()) return;
        deleteFromFile(itemToDelete.trim());
        JOptionPane.showMessageDialog(this, "Deleted entries for: " + itemToDelete);
    }

    // delete helper used by both dialog and report delete-button
    private void deleteFromFile(String itemToDelete) {
        File file = new File(DATA_FILE);
        File temp = new File("temp.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(file));
             PrintWriter pw = new PrintWriter(new FileWriter(temp))) {

            String line;
            boolean skip = false;
            while ((line = br.readLine()) != null) {
                if (!skip && line.toLowerCase().startsWith("item:") &&
                        line.substring(5).trim().equalsIgnoreCase(itemToDelete)) {
                    // start skipping this record
                    skip = true;
                    continue;
                }
                if (skip && line.contains("----")) {
                    // end of that record; stop skipping and continue
                    skip = false;
                    continue;
                }
                if (!skip) pw.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        // replace original
        if (!file.delete()) {
            System.err.println("Could not delete original data file");
            return;
        }
        if (!temp.renameTo(file)) {
            System.err.println("Could not rename temp file");
        }
    }

    private List<String[]> loadTableData() {
        List<String[]> data = new ArrayList<>();
        File f = new File(DATA_FILE);
        if (!f.exists()) return data;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line, item = "", category = "", outcome = "", dateTime = "";
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Item:")) item = line.substring(5).trim();
                else if (line.startsWith("Category:")) category = line.substring(9).trim();
                else if (line.startsWith("Outcome:")) outcome = line.substring(8).trim();
                else if (line.startsWith("Date & Time:")) dateTime = line.substring(12).trim();
                else if (line.contains("----")) {
                    data.add(new String[]{item, category, outcome, dateTime});
                    item = category = outcome = dateTime = "";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error reading data file.");
        }
        return data;
    }

    // ----------------- View Report (with Analytics + Export) -----------------
    private void showReport() {
        JFrame reportFrame = new JFrame("Waste Report Dashboard");
        reportFrame.setSize(1000, 700);
        reportFrame.setLayout(new BorderLayout(10, 10));
        reportFrame.getContentPane().setBackground(new Color(245, 247, 255));
        reportFrame.setLocationRelativeTo(null);

        // ----- NORTH: Search + Export + Analytics button -----
        JPanel topControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topControls.setBackground(new Color(220, 230, 250));
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchField = new JTextField(25);
        JButton searchButton = new JButton("Search");
        styleButton(searchButton, new Color(66, 133, 244));
        JButton analyticsButton = new JButton("View Analytics");
        styleButton(analyticsButton, new Color(33, 150, 243));
        JButton exportCsvButton = new JButton("Export CSV");
        styleButton(exportCsvButton, new Color(52, 168, 83));

        // ❌ REMOVED Export PDF Button creation

        topControls.add(searchLabel);
        topControls.add(searchField);
        topControls.add(searchButton);
        topControls.add(analyticsButton);
        topControls.add(exportCsvButton);
        // ❌ REMOVED .add(exportPdfButton);

        // ----- TABLE (center) -----
        String[] columns = {"Item", "Category", "Outcome", "Date & Time"};
        List<String[]> tableData = loadTableData();
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (String[] r : tableData) model.addRow(r);

        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(26);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Stored Waste Data"));

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // live search
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            private void filter() {
                String q = searchField.getText().trim();
                if (q.isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + q));
            }
        });

        searchButton.addActionListener(e -> {
            String q = searchField.getText().trim();
            if (q.isEmpty()) sorter.setRowFilter(null);
            else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + q));
        });

        // ----- SOUTH: Delete Selected + Back button -----
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(new Color(245, 247, 255));
        JButton deleteSelected = new JButton("Delete Selected");
        styleButton(deleteSelected, new Color(219, 68, 55));
        JButton backButton = new JButton("Back");
        styleButton(backButton, new Color(120, 144, 156));
        bottomPanel.add(deleteSelected);
        bottomPanel.add(backButton);

        // delete selected action
        deleteSelected.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(reportFrame, "Select a row to delete!");
                return;
            }
            int modelRow = table.convertRowIndexToModel(sel);
            String itemToDelete = model.getValueAt(modelRow, 0).toString();

            int confirm = JOptionPane.showConfirmDialog(reportFrame,
                    "Are you sure you want to delete '" + itemToDelete + "'?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            // remove from model (visual)
            model.removeRow(modelRow);

            // remove from file (persistent)
            deleteFromFile(itemToDelete);

            JOptionPane.showMessageDialog(reportFrame, "Deleted entries for: " + itemToDelete);
        });

        backButton.addActionListener(e -> reportFrame.dispose());

        // ----- Analytics Button Action (opens analytics frame) -----
        analyticsButton.addActionListener(e -> {
            // compute counts from current model (ensures reflects any deletions)
            Map<String, Integer> counts = new LinkedHashMap<>();
            counts.put("Recyclable Waste", 0);
            counts.put("E-Waste", 0);
            counts.put("Hazardous Waste", 0);
            counts.put("Non-Recyclable Waste", 0);

            for (int i = 0; i < model.getRowCount(); i++) {
                String cat = model.getValueAt(i, 1).toString();
                counts.put(cat, counts.getOrDefault(cat, 0) + 1);
            }
            showAnalytics(counts);
        });

        // ----- Export CSV -----
        exportCsvButton.addActionListener(e -> {
            List<String[]> rows = new ArrayList<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                String item = model.getValueAt(i, 0).toString();
                String cat = model.getValueAt(i, 1).toString();
                String outc = model.getValueAt(i, 2).toString();
                String dt = model.getValueAt(i, 3).toString();
                rows.add(new String[]{item, cat, outc, dt});
            }
            boolean ok = exportToCSV(rows, "waste_report.csv");
            if (ok) JOptionPane.showMessageDialog(reportFrame, "Exported CSV to waste_report.csv");
            else JOptionPane.showMessageDialog(reportFrame, "Export failed.");
        });

        // ❌ REMOVED Export PDF action

        // ----- Assemble -----
        reportFrame.add(topControls, BorderLayout.NORTH);
        reportFrame.add(scrollPane, BorderLayout.CENTER);
        reportFrame.add(bottomPanel, BorderLayout.SOUTH);

        reportFrame.setVisible(true);
    }

    /**
     * Analytics window shows bar and pie charts drawn via Swing
     */
    private void showAnalytics(Map<String, Integer> counts) {
        JFrame frame = new JFrame("Analytics Dashboard");
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(12, 12));
        frame.getContentPane().setBackground(new Color(245, 247, 250));

        // Header cards
        JPanel cardPanel = new JPanel(new GridLayout(1, 4, 12, 12));
        cardPanel.setBackground(new Color(245, 247, 250));
        String[] types = {"Recyclable Waste", "E-Waste", "Hazardous Waste", "Non-Recyclable Waste"};
        Color[] colors = {new Color(67,160,71), new Color(66,133,244), new Color(244,67,54), new Color(255,152,0)};

        for (int i = 0; i < types.length; i++) {
            String t = types[i];
            Color c = colors[i];
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createLineBorder(c, 2));
            JLabel title = new JLabel(t, SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 14));
            title.setForeground(c);
            JLabel val = new JLabel(String.valueOf(counts.getOrDefault(t, 0)), SwingConstants.CENTER);
            val.setFont(new Font("Segoe UI", Font.BOLD, 24));
            card.add(title, BorderLayout.NORTH);
            card.add(val, BorderLayout.CENTER);
            cardPanel.add(card);
        }

        // Chart area: left = bar, right = pie
        JPanel charts = new JPanel(new GridLayout(1, 2, 12, 12));
        charts.setBackground(new Color(245, 247, 250));
        charts.add(new BarChartPanel(counts, new String[]{"Recyclable Waste","E-Waste","Hazardous Waste","Non-Recyclable Waste"},
                new Color[]{new Color(76,175,80), new Color(66,133,244), new Color(244,67,54), new Color(255,152,0)}));
        charts.add(new PieChartPanel(counts, new String[]{"Recyclable Waste","E-Waste","Hazardous Waste","Non-Recyclable Waste"},
                new Color[]{new Color(76,175,80), new Color(66,133,244), new Color(244,67,54), new Color(255,152,0)}));

        frame.add(cardPanel, BorderLayout.NORTH);
        frame.add(charts, BorderLayout.CENTER);

        // Bottom export hint
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(new Color(245, 247, 250));
        JLabel hint = new JLabel("Export report from View Report → Export CSV / Export PDF");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        bottom.add(hint);
        frame.add(bottom, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    /**
     * Simple bar chart drawn with Graphics2D with percentage labels
     */
    private class BarChartPanel extends JPanel {
        private final Map<String,Integer> counts;
        private final String[] labels;
        private final Color[] colors;

        BarChartPanel(Map<String,Integer> counts, String[] labels, Color[] colors) {
            this.counts = counts;
            this.labels = labels;
            this.colors = colors;
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createTitledBorder("Category Counts (Bar Chart)"));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int padding = 40;
            int bottom = h - 80; // Increased bottom space for additional labels

            // Calculate total and percentages
            int total = 0;
            for (String key : labels) total += counts.getOrDefault(key, 0);
            
            int max = 1;
            for (String key : labels) max = Math.max(max, counts.getOrDefault(key, 0));
            max = Math.max(max, 1);

            int barWidth = (w - 2*padding) / labels.length - 20;
            int x = padding + 20;

            for (int i = 0; i < labels.length; i++) {
                String lbl = labels[i];
                int val = counts.getOrDefault(lbl, 0);
                int barHeight = (int) ((double) val / max * (bottom - 80));
                g.setColor(colors[i]);
                g.fillRect(x, bottom - barHeight, barWidth, barHeight);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(x, bottom - barHeight, barWidth, barHeight);

                // Calculate percentage
                double percentage = total > 0 ? (val * 100.0 / total) : 0;
                String percentageText = String.format("%.1f%%", percentage);

                // Category label
                g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                FontMetrics fm = g.getFontMetrics();
                int labelWidth = fm.stringWidth(lbl);
                g.drawString(lbl, x + (barWidth - labelWidth)/2, bottom + 20);

                // Count value above bar
                String sval = String.valueOf(val);
                int vw = fm.stringWidth(sval);
                g.drawString(sval, x + (barWidth - vw)/2, bottom - barHeight - 25);

                // Percentage above count
                g.setFont(new Font("Segoe UI", Font.BOLD, 11));
                int pw = g.getFontMetrics().stringWidth(percentageText);
                g.drawString(percentageText, x + (barWidth - pw)/2, bottom - barHeight - 10);

                x += barWidth + 30;
            }

            // Draw total count at the bottom
            if (total > 0) {
                g.setColor(Color.BLACK);
                g.setFont(new Font("Segoe UI", Font.BOLD, 14));
                String totalText = "Total Items: " + total;
                int tw = g.getFontMetrics().stringWidth(totalText);
                g.drawString(totalText, (w - tw)/2, h - 20);
            }
        }
    }

    /**
     * Simple pie chart drawn with Graphics2D with percentage labels
     */
    private class PieChartPanel extends JPanel {
        private final Map<String,Integer> counts;
        private final String[] labels;
        private final Color[] colors;

        PieChartPanel(Map<String,Integer> counts, String[] labels, Color[] colors) {
            this.counts = counts;
            this.labels = labels;
            this.colors = colors;
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createTitledBorder("Category Distribution (Pie Chart)"));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            int diameter = Math.min(w, h) - 120;
            int x = 20;
            int y = 20;

            int total = 0;
            for (String lbl : labels) total += counts.getOrDefault(lbl, 0);
            if (total == 0) {
                g.setColor(Color.GRAY);
                g.drawString("No data to display", w/2 - 40, h/2);
                return;
            }

            // Draw pie chart
            int start = 0;
            for (int i = 0; i < labels.length; i++) {
                int val = counts.getOrDefault(labels[i], 0);
                int angle = (int) Math.round(val * 360.0 / total);
                g.setColor(colors[i]);
                g.fillArc(x, y, diameter, diameter, start, angle);
                
                // Draw percentage in the center of each slice
                if (angle > 15) { // Only show percentage if slice is big enough
                    double percentage = (val * 100.0 / total);
                    String percentText = String.format("%.1f%%", percentage);
                    
                    // Calculate position for percentage text
                    double midAngle = Math.toRadians(start + angle/2.0);
                    int textX = x + diameter/2 + (int)(diameter/4 * Math.cos(midAngle));
                    int textY = y + diameter/2 - (int)(diameter/4 * Math.sin(midAngle));
                    
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    FontMetrics fm = g.getFontMetrics();
                    int textWidth = fm.stringWidth(percentText);
                    g.drawString(percentText, textX - textWidth/2, textY + fm.getAscent()/2 - 2);
                }
                
                g.setColor(Color.DARK_GRAY);
                g.drawArc(x, y, diameter, diameter, start, angle);
                start += angle;
            }

            // legend with counts and percentages
            int lx = x + diameter + 20;
            int ly = y + 10;
            g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            for (int i = 0; i < labels.length; i++) {
                int val = counts.getOrDefault(labels[i], 0);
                double percentage = total > 0 ? (val * 100.0 / total) : 0;
                
                g.setColor(colors[i]);
                g.fillRect(lx, ly + i*24, 16, 16);
                g.setColor(Color.BLACK);
                String lbl = labels[i] + " (" + val + " - " + String.format("%.1f%%", percentage) + ")";
                g.drawString(lbl, lx + 22, ly + 12 + i*24);
            }

            // Draw total in the center of pie chart
            g.setColor(Color.BLACK);
            g.setFont(new Font("Segoe UI", Font.BOLD, 16));
            String totalText = "Total: " + total;
            FontMetrics fm = g.getFontMetrics();
            int totalWidth = fm.stringWidth(totalText);
            g.drawString(totalText, x + diameter/2 - totalWidth/2, y + diameter/2 + fm.getAscent()/2 - 8);
        }
    }

    /**
     * Export rows to CSV file (no external libs)
     */
    private boolean exportToCSV(List<String[]> rows, String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("Item,Category,Outcome,Date & Time");
            for (String[] r : rows) {
                // escape simple commas by wrapping fields in quotes if needed
                pw.printf("\"%s\",\"%s\",\"%s\",\"%s\"%n", escapeCsv(r[0]), escapeCsv(r[1]), escapeCsv(r[2]), escapeCsv(r[3]));
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        return s.replace("\"","\"\"");
    }

    /**
     * Simple PDF export: saves nicely formatted plain-text into file with .pdf extension.
     * This is NOT a true typeset PDF but opens in PDF readers as text.
     */
    private boolean exportToSimplePDF(List<String[]> rows, String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("WASTE REPORT");
            pw.println("Generated on: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
            pw.println("------------------------------------------------------------");
            pw.printf("%-25s %-20s %-35s %-20s%n", "Item", "Category", "Outcome", "Date & Time");
            pw.println("------------------------------------------------------------");
            for (String[] r : rows) {
                String item = shorten(r[0],25);
                String cat = shorten(r[1],20);
                String outc = shorten(r[2],35);
                String dt = shorten(r[3],20);
                pw.printf("%-25s %-20s %-35s %-20s%n", item, cat, outc, dt);
            }
            pw.println("------------------------------------------------------------");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String shorten(String s, int len) {
        if (s == null) return "";
        if (s.length() <= len) return s;
        return s.substring(0, len-3) + "...";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WasteClassifierGUI().setVisible(true));
    }
}