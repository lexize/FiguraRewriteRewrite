package org.moon.figura.lua;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import org.moon.figura.FiguraMod;
import org.moon.figura.config.Config;
import org.moon.figura.lua.api.EventsAPI;
import org.moon.figura.lua.api.MatricesAPI;
import org.moon.figura.lua.api.VectorsAPI;
import org.terasology.jnlua.JavaFunction;
import org.terasology.jnlua.LuaRuntimeException;
import org.terasology.jnlua.LuaState53;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class FiguraLuaState extends LuaState53 {

    private static String sandboxerScript;

    public EventsAPI events;

    public FiguraLuaState(int memory) {
        super(memory * 1_000_000); //memory is given in mb
        setJavaReflector(FiguraJavaReflector.INSTANCE);
        setConverter(FiguraConverter.INSTANCE);

        //Load the built-in figura libraries
        loadLibraries();

        //Loads print(), log(), and logTable() into the env.
        loadPrintFunctions();

        //Run the figura sandboxer script
        try {
            runSandboxer();
        } catch (Exception e) {
            FiguraMod.LOGGER.error("Failed to load script sandboxer", e);
        }

        loadFiguraApis();
    }

    public boolean init(Map<String, String> scripts, String mainScript) {
        if (scripts.size() == 0)
            return false;

        if (scripts.size() == 1) {
            Map.Entry<String, String> entry = scripts.entrySet().iterator().next();
            runScript(entry.getValue(), entry.getKey());
        } else {
            if (!scripts.containsKey(mainScript)) {
                FiguraMod.LOGGER.error("Failed to load scripts, no script with name \"" + mainScript + ".lua\"");
                return false;
            }
            pushJavaFunction(requireFunc(scripts));
            setGlobal("require");
            runScript(scripts.get(mainScript), mainScript);
        }

        return true;
    }

    public void runScript(String script, String name) {
        load(script, name);
        call(0, 0);
    }

    private void loadFiguraApis() {
        loadGlobal(VectorsAPI.getInstance(), "vectors");
        loadGlobal(MatricesAPI.getInstance(), "matrices");
        events = new EventsAPI();
        loadGlobal(events, "events");
    }

    public void loadGlobal(Object api, String name) {
        pushJavaObject(api);
        setGlobal(name);
    }

    private void loadLibraries() {
        openLib(Library.BASE);
        openLib(Library.TABLE);
        openLib(Library.STRING);
        openLib(Library.MATH);
        pop(4);
    }

    private void runSandboxer() throws IOException {
        if (sandboxerScript == null) {
            InputStream stream = FiguraMod.class.getResourceAsStream("/assets/figura/scripts/sandbox.lua");
            if (stream == null)
                throw new IOException("Cannot locate sandbox.lua at /assets/figura/scripts/sandbox.lua");
            sandboxerScript = new String(stream.readAllBytes());
        }
        load(sandboxerScript, "sandboxer");
        call(0, 0);
    }

    private void loadPrintFunctions() {
        pushJavaFunction(PRINT_FUNCTION);
        pushValue(-1);
        setGlobal("log");
        setGlobal("print");
    }

    private static final Component LUA_INFO_PREFIX = new TextComponent("")
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.BLUE)
            .append("[lua] ");

    private static final Component LUA_ERROR_PREFIX = new TextComponent("")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.RED)
            .append("[ERROR] ");

    private static JavaFunction requireFunc(Map<String, String> scripts) {
        return luaState -> {
            String scriptName = luaState.checkString(1);
            if (scriptName.endsWith(".lua")) scriptName = scriptName.substring(0, scriptName.length() - 4);

            if (scripts.containsKey(scriptName)) {
                String src = scripts.get(scriptName);
                scripts.remove(scriptName);
                luaState.load(src, scriptName);
                luaState.call(0, MULTRET);
                return Math.min(luaState.getTop(), 1); //not sure if correct
            } else {
                throw new LuaRuntimeException("Failed to require " + scriptName + ". " +
                        "Either this file doesn't exist, or you've already required it before.");
            }
        };
    }

    //add a chat message on the client
    public static void sendLuaMessage(String message) {
        if ((int) Config.LOG_LOCATION.value == 0) {
            MutableComponent component = LUA_INFO_PREFIX.copy().withStyle(ChatFormatting.RESET).append(message);
            if (Minecraft.getInstance().gui != null)
                Minecraft.getInstance().gui.getChat().addMessage(component);
        } else {
            FiguraMod.LOGGER.info("[LUA] " + message);
        }
    }

    public static void sendLuaError(String error) {
        if ((int) Config.LOG_LOCATION.value == 0) {
            MutableComponent component = LUA_ERROR_PREFIX.copy().withStyle(ChatFormatting.RESET, ChatFormatting.RED).append(error);
            if (Minecraft.getInstance().gui != null)
                Minecraft.getInstance().gui.getChat().addMessage(component);
        } else {
            FiguraMod.LOGGER.error("[ERROR] " + error);
        }
    }

    private static final JavaFunction PRINT_FUNCTION = luaState -> {
        luaState.getGlobal("tostring");
        luaState.pushValue(1);
        luaState.call(1, 1);
        String v = luaState.toString(-1);
        luaState.pop(1);

        //prints the value, either on chat (which also prints on console) or only console
        sendLuaMessage(v);

        return 0;
    };
}
