package io.github.jaymingxyz.eternalparkour.core.menu;

import io.github.jaymingxyz.eternalparkour.core.menu.community.CommunityMenu;
import io.github.jaymingxyz.eternalparkour.core.menu.community.LeaderboardsMenu;
import io.github.jaymingxyz.eternalparkour.core.menu.community.SingleLeaderboardMenu;
import io.github.jaymingxyz.eternalparkour.core.menu.lobby.LobbyMenu;
import io.github.jaymingxyz.eternalparkour.core.menu.lobby.PlayerManagementMenu;
import io.github.jaymingxyz.eternalparkour.core.menu.play.PlayMenu;
import io.github.jaymingxyz.eternalparkour.core.menu.play.SingleMenu;
import io.github.jaymingxyz.eternalparkour.core.menu.play.SpectatorMenu;
import io.github.jaymingxyz.eternalparkour.core.menu.settings.LangMenu;
import io.github.jaymingxyz.eternalparkour.core.menu.settings.ParkourSettingsMenu;
import io.github.jaymingxyz.eternalparkour.core.menu.settings.SettingsMenu;

public class Menus {

    // main
    public static MainMenu MAIN = new MainMenu();

    // play
    public static PlayMenu PLAY = new PlayMenu();
    public static SingleMenu SINGLE = new SingleMenu();
    public static SpectatorMenu SPECTATOR = new SpectatorMenu();

    // community
    public static CommunityMenu COMMUNITY = new CommunityMenu();
    public static LeaderboardsMenu LEADERBOARDS = new LeaderboardsMenu();
    public static SingleLeaderboardMenu SINGLE_LEADERBOARD = new SingleLeaderboardMenu();

    // settings
    public static SettingsMenu SETTINGS = new SettingsMenu();
    public static LangMenu LANG = new LangMenu();
    public static ParkourSettingsMenu PARKOUR_SETTINGS = new ParkourSettingsMenu();

    // lobby
    public static LobbyMenu LOBBY = new LobbyMenu();
    public static PlayerManagementMenu PLAYER_MANAGEMENT = new PlayerManagementMenu();

}
