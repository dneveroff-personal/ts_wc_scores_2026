package com.tswcscores.bot;

import com.tswcscores.entity.Team;
import com.tswcscores.entity.User;
import com.tswcscores.repository.MatchRepository;
import com.tswcscores.service.ScoringService;
import com.tswcscores.service.impl.FootballDataService;
import com.tswcscores.service.impl.GroupService;
import com.tswcscores.service.impl.PredictionService;
import com.tswcscores.service.impl.UserService;
import com.tswcscores.telegram.TelegramBotClient;
import com.tswcscores.telegram.TelegramUpdate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WcPredictBotTest {

    @Test
    void handleUpdateWithPredictCommandSavesPredictionAndSendsMessage() {
        TelegramBotClient telegram = mock(TelegramBotClient.class);
        UserService userService = mock(UserService.class);
        PredictionService predictionService = mock(PredictionService.class);
        MatchRepository matchRepository = mock(MatchRepository.class);
        FootballDataService footballDataService = mock(FootballDataService.class);
        ScoringService scoringService = mock(ScoringService.class);
        GroupService groupService = mock(GroupService.class);

        User user = new User();
        user.setId(1L);
        user.setTelegramId(42L);
        when(userService.findByTelegramId(42L)).thenReturn(Optional.of(user));

        Team homeTeam = new Team();
        homeTeam.setId(1L);
        homeTeam.setTla("RUS");
        homeTeam.setShortName("Russia");

        Team awayTeam = new Team();
        awayTeam.setId(2L);
        awayTeam.setTla("BRA");
        awayTeam.setShortName("Brazil");

        com.tswcscores.entity.Match match = new com.tswcscores.entity.Match();
        match.setId(123L);
        match.setStatus(com.tswcscores.entity.Match.Status.SCHEDULED);
        match.setUtcDate(java.time.LocalDateTime.now().plusHours(2));
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);

        com.tswcscores.entity.Prediction prediction = new com.tswcscores.entity.Prediction();
        prediction.setId(1L);
        prediction.setMatch(match);
        prediction.setHomeScore(2);
        prediction.setAwayScore(1);
        when(predictionService.savePrediction(any(User.class), anyLong(), anyInt(), anyInt())).thenReturn(prediction);

        WcPredictBot bot = new WcPredictBot(
                telegram,
                userService,
                predictionService,
                matchRepository,
                footballDataService,
                scoringService,
                groupService,
                null
        );

        TelegramUpdate.TelegramMessage msg = new TelegramUpdate.TelegramMessage();
        msg.setText("/predict 123 2 1");

        TelegramUpdate.TelegramUser from = new TelegramUpdate.TelegramUser();
        from.setId(42L);
        msg.setFrom(from);

        TelegramUpdate.TelegramChat chat = new TelegramUpdate.TelegramChat();
        chat.setId(42L);
        msg.setChat(chat);

        TelegramUpdate update = new TelegramUpdate();
        update.setMessage(msg);

        bot.handleUpdate(update);

        verify(predictionService).savePrediction(any(User.class), eq(123L), eq(2), eq(1));
        verify(telegram).sendMessage(eq(42L), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void handleUpdateWithCallbackQueryIsIgnored() {
        TelegramBotClient telegram = mock(TelegramBotClient.class);
        UserService userService = mock(UserService.class);
        PredictionService predictionService = mock(PredictionService.class);
        MatchRepository matchRepository = mock(MatchRepository.class);
        FootballDataService footballDataService = mock(FootballDataService.class);
        ScoringService scoringService = mock(ScoringService.class);
        GroupService groupService = mock(GroupService.class);

        WcPredictBot bot = new WcPredictBot(
                telegram,
                userService,
                predictionService,
                matchRepository,
                footballDataService,
                scoringService,
                groupService,
                null
        );

        TelegramUpdate.TelegramCallbackQuery callback = new TelegramUpdate.TelegramCallbackQuery();
        callback.setId("callback-1");
        callback.setData("predict:123");

        TelegramUpdate.TelegramUser from = new TelegramUpdate.TelegramUser();
        from.setId(42L);
        callback.setFrom(from);

        TelegramUpdate.TelegramChat chat = new TelegramUpdate.TelegramChat();
        chat.setId(-100L);

        TelegramUpdate.TelegramMessage message = new TelegramUpdate.TelegramMessage();
        message.setChat(chat);
        callback.setMessage(message);

        TelegramUpdate update = new TelegramUpdate();
        update.setCallbackQuery(callback);

        bot.handleUpdate(update);

        verify(telegram, org.mockito.Mockito.never()).sendMessage(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
        verify(telegram, org.mockito.Mockito.never()).answerCallbackQuery(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyBoolean());
    }
}
