package ml.empee.commandsManager;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public abstract class AbstractCommandTest {

  protected Logger log = Logger.getLogger("MockedServer");
  protected JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
  protected CommandManager commandManager;
  protected Queue<String> senderReceivedMessage = new LinkedList<>();
  protected CommandSender sender = Mockito.mock(Player.class);

  @BeforeEach
  public void setUp() {
    Mockito.when(plugin.getLogger()).thenReturn(log);

    senderReceivedMessage.clear();
    commandManager = new CommandManager(plugin);

    sender = Mockito.mock(Player.class);
    when(sender.getName()).thenReturn("MockedPlayer");
    when(sender.hasPermission(Mockito.anyString())).thenReturn(true);

    doAnswer((invocation) -> {
      log.info(invocation.getArguments()[0].toString());
      return senderReceivedMessage.add(invocation.getArguments()[0].toString());
    }).when(sender).sendMessage(Mockito.anyString());
  }

}