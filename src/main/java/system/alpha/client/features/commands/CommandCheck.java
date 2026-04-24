package system.alpha.client.features.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import system.alpha.api.command.Command;
import system.alpha.api.command.CommandRegister;
import system.alpha.client.services.CheckService;

@CommandRegister(name = "check")
public class CommandCheck extends Command {
    private final CheckService checkManager = CheckService.getInstance();

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("addtime").then(argument("player", StringArgumentType.word()).then(argument("seconds", IntegerArgumentType.integer(1)).executes(context -> {
            String player = StringArgumentType.getString(context, "player");
            int seconds = IntegerArgumentType.getInteger(context, "seconds");

            if (!checkManager.isBeingChecked(player)) {
                print("Игрок " + player + " не на проверке!");
                return 0;
            }

            checkManager.addTime(player, seconds * 1000L);
            long remaining = checkManager.getCheck(player).getRemainingTime();
            print("Добавлено " + seconds + " секунд игроку " + player);
            print("Осталось времени: " + formatTime(remaining));
            return SINGLE_SUCCESS;
        }))));

        builder.then(literal("settime").then(argument("player", StringArgumentType.word()).then(argument("seconds", IntegerArgumentType.integer(1)).executes(context -> {
            String player = StringArgumentType.getString(context, "player");
            int seconds = IntegerArgumentType.getInteger(context, "seconds");

            if (!checkManager.isBeingChecked(player)) {
                print("Игрок " + player + " не на проверке!");
                return 0;
            }

            checkManager.setTime(player, seconds * 1000L);
            print("Установлено " + seconds + " секунд игроку " + player);
            return SINGLE_SUCCESS;
        }))));

        builder.then(literal("settimeadditions").then(argument("seconds", IntegerArgumentType.integer(1)).executes(context -> {
            int seconds = IntegerArgumentType.getInteger(context, "seconds");
            checkManager.setAdditionalTimeAmount(seconds * 1000L);
            print("Установлено дополнительное время: " + seconds + " секунд");
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("setdefaulttime").then(argument("seconds", IntegerArgumentType.integer(1)).executes(context -> {
            int seconds = IntegerArgumentType.getInteger(context, "seconds");
            checkManager.setDefaultTime(seconds * 1000L);
            print("Установлено время проверки по умолчанию: " + seconds + " секунд");
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("list").executes(context -> {
            print("Активные проверки:");
            if (checkManager.getActiveChecks().isEmpty()) {
                print("Нет активных проверок");
            } else {
                for (CheckService.CheckSession session : checkManager.getActiveChecks()) {
                    if (!session.isExpired()) {
                        print("Игрок: " + session.getPlayerName() + " | Осталось: " + session.getFormattedTime());
                    }
                }
            }
            return SINGLE_SUCCESS;
        }));
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}