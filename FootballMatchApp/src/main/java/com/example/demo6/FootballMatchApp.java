package com.example.demo6;
import com.google.gson.Gson;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FootballMatchApp extends Application {
  private static final String API_KEY = "729b62e096eb4745bc089ccd825896e5";
  private static final String BASE_URL = "https://api.football-data.org/v2/competitions/";

  private TabPane tabPane = new TabPane();
  private Map<String, VBox> leagueTabs = new HashMap<>();

  @Override
  public void start(Stage primaryStage) {
    StackPane root = new StackPane();
    Scene scene = new Scene(root, 800, 600);
    scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

    tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

    addLeagueTab("PL", "Premier League");
    addLeagueTab("BL1", "Bundesliga");
    addLeagueTab("SA", "Serie A");
    addLeagueTab("PD", "La Liga");
    addLeagueTab("FL1", "Ligue 1");

    VBox vbox = new VBox();
    vbox.setSpacing(10);
    vbox.setPadding(new Insets(10));
    vbox.getChildren().addAll(tabPane);

    root.getChildren().add(vbox);
    primaryStage.setScene(scene);
    primaryStage.setTitle("Football Match App");
    primaryStage.show();
  }

  private void addLeagueTab(String leagueCode, String leagueName) {
    VBox vbox = new VBox();
    vbox.setSpacing(10);
    vbox.setPadding(new Insets(10));

    Label label = new Label("Выберите дату:");
    DatePicker datePicker = new DatePicker();
    Button getMatchesButton = new Button("Получить расписание матчей");
    TextArea matchesTextArea = new TextArea();

    ChoiceBox<String> filterChoiceBox = new ChoiceBox<>();
    filterChoiceBox.getItems().addAll("Все", "Запланированные", "Завершенные", "Идущие");

    filterChoiceBox.setOnAction(e -> {
      String selectedFilter = filterChoiceBox.getValue();
      LocalDate selectedDate = datePicker.getValue();
      if (selectedFilter != null && selectedDate != null) {
        String matchesData = getUpcomingMatches(leagueCode, selectedDate, selectedFilter);
        matchesTextArea.setText(matchesData);
      }
    });

    getMatchesButton.setOnAction(e -> {
      LocalDate selectedDate = datePicker.getValue();
      String selectedFilter = filterChoiceBox.getValue();
      if (selectedDate != null) {
        String matchesData = getUpcomingMatches(leagueCode, selectedDate, selectedFilter);
        matchesTextArea.setText(matchesData);
      } else {
        matchesTextArea.setText("Выберите дату!");
      }
    });

    vbox.getChildren().addAll(label, datePicker, filterChoiceBox, getMatchesButton, matchesTextArea);
    leagueTabs.put(leagueCode, vbox);

    Tab tab = new Tab(leagueName);
    tab.setContent(vbox);
    tab.setClosable(false);

    tabPane.getTabs().add(tab);
  }

  private String getUpcomingMatches(String leagueCode, LocalDate selectedDate, String filter) {
    StringBuilder response = new StringBuilder();

    try {
      URL url = new URL(BASE_URL + leagueCode + "/matches?status=SCHEDULED");
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("X-Auth-Token", API_KEY);

      BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String inputLine;

      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();

      Gson gson = new Gson();
      Map<String, Object> jsonData = gson.fromJson(response.toString(), Map.class);
      String matches = parseMatchesData(jsonData, selectedDate, filter);

      return matches;
    } catch (Exception e) {
      e.printStackTrace();
      return "Ошибка при получении данных";
    }
  }

  private String parseMatchesData(Map<String, Object> jsonData, LocalDate selectedDate, String filter) {
    StringBuilder matchesInfo = new StringBuilder();

    if (jsonData.containsKey("matches")) {
      List<Map<String, Object>> matchesList = (List<Map<String, Object>>) jsonData.get("matches");
      for (Map<String, Object> match : matchesList) {
        String date = match.get("utcDate").toString();
        LocalDate matchDate = LocalDate.parse(date.substring(0, 10));
        if (matchDate.equals(selectedDate) && isMatchFilterMatched(match, filter)) {
          String homeTeam = ((Map<String, Object>) match.get("homeTeam")).get("name").toString();
          String awayTeam = ((Map<String, Object>) match.get("awayTeam")).get("name").toString();

          matchesInfo.append(date).append(" - ").append(homeTeam).append(" vs ").append(awayTeam).append("\n");
        }
      }
    } else {
      matchesInfo.append("Нет предстоящих матчей");
    }

    return matchesInfo.toString();
  }

  private boolean isMatchFilterMatched(Map<String, Object> match, String filter) {
    if ("Все".equals(filter)) {
      return true;
    }

    if ("Запланированные".equals(filter)) {
      String status = match.get("status").toString();
      return "SCHEDULED".equals(status);
    }

    return false;
  }
  private static class MatchInfo {
    private String date;
    private String homeTeam;
    private String awayTeam;

    public MatchInfo(String date, String homeTeam, String awayTeam) {
      this.date = date;
      this.homeTeam = homeTeam;
      this.awayTeam = awayTeam;
    }

    public String getDate() {
      return date;
    }

    public String getHomeTeam() {
      return homeTeam;
    }

    public String getAwayTeam() {
      return awayTeam;
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}

