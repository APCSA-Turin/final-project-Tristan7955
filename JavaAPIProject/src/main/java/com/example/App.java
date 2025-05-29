package com.example;

import com.example.util.RoundedBorder;
import com.example.util.RoundedButton;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

public class App extends JFrame {
    private static final Color PRIMARY_COLOR = new Color(0x4D6BFE);
    private static final Color BACKGROUND_COLOR = new Color(0x292A2D);
    private static final Color TEXTFIELD_COLOR = new Color(0x404045);
    private static final Color SIDEBAR_COLOR = new Color(0x212327);
    private static final Color CARD_COLOR = new Color(0x35363A);
    private static final Color TEXT_PRIMARY = new Color(0xFFFFFF);
    private static final Color TEXT_SECONDARY = new Color(0xB0B0B0);
    private static final Color BORDER_COLOR = new Color(0x404045);
    private static final Color CURSOR_DOT_COLOR = new Color(0x4D6BFE);
    private static final Color BUY_COLOR = new Color(0x4CAF50);
    private static final Color SELL_COLOR = new Color(0xF44336);
    private static final Color BUTTON_HOVER = new Color(0x6D8BFF);
    private static final Color BUTTON_PRESSED = new Color(0x3A56D4);
    //Map of popular stock symbols and their company names
    private static final Map<String, String> COMPANY_SYMBOL_MAP = new LinkedHashMap<>() {{
        put("Apple", "AAPL");
        put("Microsoft", "MSFT");
        put("Google", "GOOGL");
        put("Amazon", "AMZN");
        put("Tesla", "TSLA");
        put("Meta", "META");
        put("Nvidia", "NVDA");
        put("Netflix", "NFLX");
        put("Adobe", "ADBE");
        put("Intel", "INTC");
    }};
    private final String API_KEY = "1N4RP0PPLH4NJ5RU";
    private final ChartPanel chartPanel;
    private final JPanel mainPanel;
    private final JWindow tooltipWindow;
    private final JLabel tooltipContent;
    private final JLabel recommendationLabel;
    private final JPanel recommendationPanel;
    private JTextField searchField;
    private JButton searchButton;
    private JLabel statusLabel;
    private JPanel sidebarPanel;
    private Point cursorDotPosition;
    private double currentYValue;

    public App() {
        super("Echo Viewer");
        setIconImage(new ImageIcon(getClass().getResource("/logo.png")).getImage());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        createSidebar();

        JPanel searchPanel = createSearchPanel();

        tooltipWindow = new JWindow();
        tooltipContent = new JLabel("", SwingConstants.CENTER);
        tooltipContent.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tooltipContent.setForeground(TEXT_PRIMARY);
        tooltipContent.setBackground(CARD_COLOR);
        tooltipContent.setOpaque(true);
        tooltipContent.setBorder(new RoundedBorder(10, BORDER_COLOR));
        tooltipContent.setPreferredSize(new Dimension(180, 35));
        tooltipWindow.getContentPane().add(tooltipContent);
        tooltipWindow.setAlwaysOnTop(true);

        chartPanel = new ChartPanel(null) {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (cursorDotPosition != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    Rectangle2D dataArea = chartPanel.getScreenDataArea();

                    if (dataArea.contains(cursorDotPosition)) {
                        g2.setColor(CURSOR_DOT_COLOR);
                        g2.fillOval(cursorDotPosition.x - 5, cursorDotPosition.y - 5, 10, 10);

                        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
                        g2.setColor(new Color(255, 255, 255, 100));
                        g2.drawLine(
                                cursorDotPosition.x,
                                (int) dataArea.getMinY(),
                                cursorDotPosition.x,
                                (int) dataArea.getMaxY()
                        );
                    }
                    g2.dispose();
                }
            }
        };
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setBackground(CARD_COLOR);
        chartPanel.setBorder(new RoundedBorder(15, BORDER_COLOR));
        chartPanel.addMouseMotionListener(new ChartMouseListener());
        chartPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                cursorDotPosition = null;
                tooltipWindow.setVisible(false);
                chartPanel.repaint();
            }
        });

        recommendationPanel = new JPanel(new BorderLayout());
        recommendationPanel.setBackground(CARD_COLOR);
        recommendationPanel.setBorder(new RoundedBorder(15, BORDER_COLOR));
        recommendationPanel.setPreferredSize(new Dimension(0, 60));

        JLabel recTitle = new JLabel("Investment Recommendation:");
        recTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        recTitle.setForeground(TEXT_SECONDARY);
        recTitle.setBorder(new EmptyBorder(5, 15, 5, 5));

        recommendationLabel = new JLabel("Search a stock to get recommendation");
        recommendationLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        recommendationLabel.setForeground(TEXT_PRIMARY);
        recommendationLabel.setBorder(new EmptyBorder(5, 15, 5, 15));

        recommendationPanel.add(recTitle, BorderLayout.NORTH);
        recommendationPanel.add(recommendationLabel, BorderLayout.CENTER);

        mainPanel.add(searchPanel, BorderLayout.NORTH);
        mainPanel.add(chartPanel, BorderLayout.CENTER);
        mainPanel.add(recommendationPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
        add(sidebarPanel, BorderLayout.WEST);

        updateStatus("Enter a company name or stock symbol", false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            App viewer = new App();
            viewer.setVisible(true);
        });
    }

    private void createSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(SIDEBAR_COLOR);
        sidebarPanel.setPreferredSize(new Dimension(280, getHeight()));
        sidebarPanel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 0, 1, BORDER_COLOR),
                new EmptyBorder(20, 15, 20, 15)
        ));

        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(SIDEBAR_COLOR);
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));

        JLabel titleLabel = new JLabel("Echo Viewer");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        titlePanel.add(Box.createHorizontalGlue());
        titlePanel.add(Box.createHorizontalStrut(10));
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createHorizontalGlue());

        sidebarPanel.add(titlePanel);
        sidebarPanel.add(Box.createVerticalStrut(30));

        JLabel sectionLabel = new JLabel("Popular Stocks");
        sectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sectionLabel.setForeground(TEXT_SECONDARY);
        sectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sectionLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        sidebarPanel.add(sectionLabel);

        for (Map.Entry<String, String> entry : COMPANY_SYMBOL_MAP.entrySet()) {
            JButton stockButton = createStockButton(entry.getKey(), entry.getValue());
            stockButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            sidebarPanel.add(stockButton);
            sidebarPanel.add(Box.createVerticalStrut(8));
        }

        sidebarPanel.add(Box.createVerticalGlue());
    }

    private JButton createStockButton(String company, String symbol) {
        RoundedButton button = new RoundedButton(
                String.format("%s (%s)", company, symbol),
                20,
                PRIMARY_COLOR,
                BUTTON_HOVER,
                BUTTON_PRESSED
        );

        button.setPreferredSize(new Dimension(200, 40));
        button.setMaximumSize(new Dimension(200, 40));
        button.setHorizontalAlignment(SwingConstants.CENTER);

        button.addActionListener(e -> {
            searchField.setText(symbol);
            searchStock();
        });

        return button;
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new BorderLayout(10, 10));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JPanel searchContainer = new JPanel(new BorderLayout(10, 10));
        searchContainer.setBackground(CARD_COLOR);
        searchContainer.setBorder(new RoundedBorder(15, BORDER_COLOR));

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setBackground(TEXTFIELD_COLOR);
        searchField.setCaretColor(TEXT_PRIMARY);
        searchField.setBorder(new RoundedBorder(10, BORDER_COLOR));
        searchField.addActionListener(e -> searchStock());

        searchButton = new RoundedButton(
                "Search",
                10,
                PRIMARY_COLOR,
                BUTTON_HOVER,
                BUTTON_PRESSED
        );
        searchButton.setPreferredSize(new Dimension(100, 40));
        searchButton.addActionListener(e -> searchStock());

        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.setOpaque(false);
        inputPanel.add(searchField, BorderLayout.CENTER);
        inputPanel.add(searchButton, BorderLayout.EAST);
        inputPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        searchContainer.add(inputPanel, BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(TEXT_SECONDARY);
        statusLabel.setBorder(new EmptyBorder(5, 15, 0, 0));

        searchPanel.add(searchContainer, BorderLayout.CENTER);
        searchPanel.add(statusLabel, BorderLayout.SOUTH);

        return searchPanel;
    }


    private void searchStock() {
        String input = searchField.getText().trim();
        if (input.isEmpty()) {
            updateStatus("Please enter a company name or stock symbol", true);
            return;
        }

        String symbol = resolveSymbol(input);
        if (symbol == null) {
            updateStatus("Company not recognized. Try using the stock symbol directly.", true);
            return;
        }

        updateStatus("Fetching data for " + symbol + "...", false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    JSONObject stockData = fetchStockData(symbol);
                    displayStockChart(symbol, stockData);
                    updateStatus("Showing data for " + symbol, false);
                } catch (Exception ex) {
                    updateStatus("Error: " + ex.getMessage(), true);
                }
                return null;
            }
        }.execute();
    }

    /*     * Fetches stock data from Alpha Vantage API
     * @param symbol The stock symbol to fetch data for
     * @return A JSONObject containing the stock data
     */
    private JSONObject fetchStockData(String symbol) throws IOException, JSONException {
        String urlString = String.format(
                "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s&outputsize=full",
                symbol, API_KEY);

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() == 429) {
            throw new IOException("API rate limit exceeded. Please wait a minute and try again.");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JSONObject jsonResponse = new JSONObject(response.toString());
            if (jsonResponse.has("Error Message")) {
                throw new IOException(jsonResponse.getString("Error Message"));
            }
            if (jsonResponse.has("Note")) {
                throw new IOException(jsonResponse.getString("Note"));
            }

            return jsonResponse;
        }
    }

    /*     * Resolves the stock symbol from the input
     * @param input The user input which can be a company name or stock symbol
     * @return The resolved stock symbol in uppercase, or the original input if not found
     */
    private String resolveSymbol(String input) {
        if (COMPANY_SYMBOL_MAP.containsValue(input.toUpperCase())) {
            return input.toUpperCase();
        }

        for (Map.Entry<String, String> entry : COMPANY_SYMBOL_MAP.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(input)) {
                return entry.getValue();
            }
        }

        for (Map.Entry<String, String> entry : COMPANY_SYMBOL_MAP.entrySet()) {
            if (entry.getKey().toLowerCase().contains(input.toLowerCase())) {
                return entry.getValue();
            }
        }

        return input.toUpperCase();
    }

    /*     * Updates the status label with a message
     * @param message The message to display
     * @param isError Whether the message indicates an error
     */
    private void updateStatus(String message, boolean isError) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
            statusLabel.setForeground(isError ? new Color(239, 68, 68) : TEXT_SECONDARY);
        });
    }

    /*     * Displays the stock chart for the provided symbol
     * @param symbol The stock symbol to display
     * @param stockData The JSON data containing stock prices
     */
    private void displayStockChart(String symbol, JSONObject stockData) throws JSONException {
        JSONObject timeSeries = stockData.getJSONObject("Time Series (Daily)");
        TimeSeries series = new TimeSeries(symbol + " Daily Prices");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        List<String> dates = new ArrayList<>(timeSeries.keySet());
        Collections.sort(dates);

        int daysToShow = Math.min(365, dates.size());
        List<String> recentDates = dates.subList(dates.size() - daysToShow, dates.size());

        List<Double> prices = new ArrayList<>();
        for (String dateStr : recentDates) {
            JSONObject dayData = timeSeries.getJSONObject(dateStr);
            double closePrice = dayData.getDouble("4. close");
            prices.add(closePrice);

            Date date = null;
            try {
                date = dateFormat.parse(dateStr);
            } catch (Exception e) {
                continue;
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            Day day = new Day(cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.YEAR));
            series.add(day, closePrice);
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                symbol + " Stock Price (Past Year)",
                "Date",
                "Price (USD)",
                dataset,
                true,
                true,
                false
        );

        chart.setBackgroundPaint(CARD_COLOR);
        chart.getTitle().setPaint(TEXT_PRIMARY);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(CARD_COLOR);
        plot.setDomainGridlinePaint(BORDER_COLOR);
        plot.setRangeGridlinePaint(BORDER_COLOR);
        plot.getRenderer().setSeriesPaint(0, PRIMARY_COLOR);

        plot.getDomainAxis().setLabelPaint(TEXT_PRIMARY);
        plot.getDomainAxis().setTickLabelPaint(TEXT_SECONDARY);
        plot.getRangeAxis().setLabelPaint(TEXT_PRIMARY);
        plot.getRangeAxis().setTickLabelPaint(TEXT_SECONDARY);

        SwingUtilities.invokeLater(() -> {
            chartPanel.setChart(chart);
            chartPanel.revalidate();

            String recommendation = generateInvestmentRecommendation(prices);
            recommendationLabel.setText(recommendation);
            recommendationLabel.setForeground(recommendation.contains("BUY") ? BUY_COLOR :
                    recommendation.contains("SELL") ? SELL_COLOR : TEXT_PRIMARY);

            addRecommendationAnnotations(plot, prices, recentDates);
        });
    }

    /*     * Generates an investment recommendation based on multiple factors
     * @param prices List of stock prices
     * @return A string containing the recommendation
     */
    private String generateInvestmentRecommendation(List<Double> prices) {
        if (prices == null || prices.size() < 30) {
            return "Not enough data to generate recommendation";
        }

        double shortTermMA = calculateSMA(prices, 10);
        double longTermMA = calculateSMA(prices, 50);
        double currentPrice = prices.get(prices.size() - 1);

        double rsi = calculateRSI(prices, 14);

        double macd = calculateMACD(prices);

        double supportLevel = calculateSupportLevel(prices);
        double resistanceLevel = calculateResistanceLevel(prices);

        StringBuilder recommendation = new StringBuilder();

        if (shortTermMA > longTermMA && currentPrice > shortTermMA) {
            recommendation.append("Strong BUY signal (Golden Cross detected). ");
        } else if (shortTermMA < longTermMA && currentPrice < shortTermMA) {
            recommendation.append("Strong SELL signal (Death Cross detected). ");
        }

        if (rsi < 30) {
            recommendation.append("Oversold (RSI=").append(String.format("%.2f", rsi)).append("). ");
        } else if (rsi > 70) {
            recommendation.append("Overbought (RSI=").append(String.format("%.2f", rsi)).append("). ");
        }

        if (macd > 0) {
            recommendation.append("Bullish momentum (MACD positive). ");
        } else {
            recommendation.append("Bearish momentum (MACD negative). ");
        }

        if (currentPrice < supportLevel * 1.02) {
            recommendation.append("Near support level ($").append(String.format("%.2f", supportLevel)).append("). ");
        } else if (currentPrice > resistanceLevel * 0.98) {
            recommendation.append("Near resistance level ($").append(String.format("%.2f", resistanceLevel)).append("). ");
        }

        if (recommendation.length() == 0) {
            return "HOLD - No strong signals detected";
        }

        return recommendation.toString();
    }

    /*     * Adds buy and sell recommendation annotations to the plot
     * @param plot
     * @param prices
     * @param dates
     */
    private void addRecommendationAnnotations(XYPlot plot, List<Double> prices, List<String> dates) {
        List<Integer> buyPoints = new ArrayList<>();
        List<Integer> sellPoints = new ArrayList<>();

        // Find local minima and maxima for buy/sell points
        for (int i = 2; i < prices.size() - 2; i++) {
            if (prices.get(i) < prices.get(i - 1) && prices.get(i) < prices.get(i - 2) &&
                    prices.get(i) < prices.get(i + 1) && prices.get(i) < prices.get(i + 2)) {
                buyPoints.add(i);
            }
            if (prices.get(i) > prices.get(i - 1) && prices.get(i) > prices.get(i - 2) &&
                    prices.get(i) > prices.get(i + 1) && prices.get(i) > prices.get(i + 2)) {
                sellPoints.add(i);
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // Add annotations for buy/sell points
        try {
            for (int index : buyPoints) {
                Date date = dateFormat.parse(dates.get(index));
                XYTextAnnotation annotation = new XYTextAnnotation("B", date.getTime(), prices.get(index) * 0.98);


                annotation.setFont(new Font("SansSerif", Font.BOLD, 10));
                annotation.setPaint(BUY_COLOR);
                annotation.setBackgroundPaint(new Color(0, 0, 0, 0));
                plot.addAnnotation(annotation);
            }

            for (int index : sellPoints) {
                Date date = dateFormat.parse(dates.get(index));
                XYTextAnnotation annotation = new XYTextAnnotation("S", date.getTime(), prices.get(index) * 1.02);
                annotation.setFont(new Font("SansSerif", Font.BOLD, 10));
                annotation.setPaint(SELL_COLOR);
                annotation.setBackgroundPaint(new Color(0, 0, 0, 0));
                plot.addAnnotation(annotation);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*     * Calculates the Simple Moving Average for a certain period
     * @param prices List of stock prices
     * @param period The number of days to calculate the SMA over
     * @return The calculated SMA value
     */
    private double calculateSMA(List<Double> prices, int period) {
        if (prices.size() < period) return 0;
        double sum = 0;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    /*     * Calculates the Relative Strength Index for a certain period
     * @param prices List of stock prices
     * @param period The number of days to calculate the RSI over
     * @return The calculated RSI value
     */
    private double calculateRSI(List<Double> prices, int period) {
        if (prices.size() <= period) return 50;

        List<Double> gains = new ArrayList<>();
        List<Double> losses = new ArrayList<>();

        for (int i = 1; i <= period; i++) {
            double change = prices.get(prices.size() - i) - prices.get(prices.size() - i - 1);
            if (change > 0) {
                gains.add(change);
                losses.add(0.0);
            } else {
                gains.add(0.0);
                losses.add(-change);
            }
        }

        // Calculate average loss and gain
        double avgGain = gains.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgLoss = losses.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        if (avgLoss == 0) return 100;
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    /*     * Calculates the Moving Average Convergence Divergence
     * @param prices List of stock prices
     * @return The calculated MACD value
     */
    private double calculateMACD(List<Double> prices) {
        if (prices.size() < 26) return 0;

        double ema12 = calculateEMA(prices, 12);
        double ema26 = calculateEMA(prices, 26);
        return ema12 - ema26;
    }

    /*     * Calculates the Exponential Moving Average for a certain period
     * @param prices List of stock prices
     * @param period The number of days to calculate the EMA over
     * @return The calculated EMA value
     */
    private double calculateEMA(List<Double> prices, int period) {
        if (prices.size() < period) return 0;

        double ema = calculateSMA(prices.subList(0, period), period);
        double multiplier = 2.0 / (period + 1);

        for (int i = period; i < prices.size(); i++) {
            ema = (prices.get(i) - ema) * multiplier + ema;
        }

        return ema;
    }

    /*     * Calculates the support level based on the prices over the last 30 days
     * @param prices
     * @Return The calculated support level
     */
    private double calculateSupportLevel(List<Double> prices) {
        int lookback = Math.min(30, prices.size());
        return prices.subList(prices.size() - lookback, prices.size()).stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0);
    }

    /*     * Calculates the resistance level based on the prices over the last 30 days
     * @param prices
     * @Return The calculated resistance level
     */
    private double calculateResistanceLevel(List<Double> prices) {
        int lookback = Math.min(30, prices.size());
        return prices.subList(prices.size() - lookback, prices.size()).stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0);
    }




    /**
     * Mouse listener to handle cursor movement and display tooltips
     */
    class ChartMouseListener extends MouseMotionAdapter {
        @Override
        public void mouseMoved(MouseEvent e) {
            if (chartPanel.getChart() != null) {
                XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
                double x = plot.getDomainAxis().java2DToValue(
                        e.getX(),
                        chartPanel.getScreenDataArea(),
                        plot.getDomainAxisEdge()
                );

                double y = findYValueAtX(plot, x);

                int yPixel = (int) plot.getRangeAxis().valueToJava2D(
                        y,
                        chartPanel.getScreenDataArea(),
                        plot.getRangeAxisEdge()
                );

                currentYValue = y;
                cursorDotPosition = new Point(e.getX(), yPixel);
                chartPanel.repaint();

                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
                String dateStr = dateFormat.format(new Date((long) x));
                tooltipContent.setText(String.format("%s | $%.2f", dateStr, y));
                tooltipWindow.pack();
                tooltipWindow.setLocation(e.getLocationOnScreen().x + 15, e.getLocationOnScreen().y - 40);
                tooltipWindow.setVisible(true);
            }
        }

        /**
         * Finds the Y value at a given X position in the plot
         * @param plot The XYPlot containing the data
         * @param x The X value to find the corresponding Y value for
         * @return The interpolated Y value at the given X
         */
        private double findYValueAtX(XYPlot plot, double x) {
            TimeSeriesCollection dataset = (TimeSeriesCollection) plot.getDataset();
            TimeSeries series = dataset.getSeries(0);

            long targetTime = (long) x;
            int prevIndex = -1;
            int nextIndex = -1;

            for (int i = 0; i < series.getItemCount(); i++) {
                long itemTime = series.getTimePeriod(i).getStart().getTime();
                if (itemTime <= targetTime) {
                    prevIndex = i;
                } else {
                    nextIndex = i;
                    break;
                }
            }

            if (prevIndex == -1) return series.getValue(0).doubleValue();
            if (nextIndex == -1) return series.getValue(series.getItemCount() - 1).doubleValue();

            long t1 = series.getTimePeriod(prevIndex).getStart().getTime();
            long t2 = series.getTimePeriod(nextIndex).getStart().getTime();
            double y1 = series.getValue(prevIndex).doubleValue();
            double y2 = series.getValue(nextIndex).doubleValue();

            double alpha = (double) (targetTime - t1) / (t2 - t1);
            return y1 + alpha * (y2 - y1);
        }
    }
}