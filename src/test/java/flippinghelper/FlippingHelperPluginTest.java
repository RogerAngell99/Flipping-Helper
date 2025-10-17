package flippinghelper;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FlippingHelperPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(FlippingHelperPlugin.class);
        RuneLite.main(args);
    }
}
