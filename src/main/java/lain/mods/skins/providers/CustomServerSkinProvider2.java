package lain.mods.skins.providers;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lain.lib.SharedPool;
import lain.mods.skins.api.interfaces.IPlayerProfile;
import lain.mods.skins.api.interfaces.ISkin;
import lain.mods.skins.api.interfaces.ISkinProvider;
import lain.mods.skins.impl.Shared;
import lain.mods.skins.impl.SkinData;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

public class CustomServerSkinProvider2 implements ISkinProvider
{

    private Function<ByteBuffer, ByteBuffer> _filter;
    private String _host;

    public String getSkinUrlFromUrl(String url)
    {
        URL url_obj = null;
        try
        {
            url_obj = new URL(url);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
            return "";
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = url_obj.openStream()) {
          byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
          int n;

          while ( (n = is.read(byteChunk)) > 0 ) {
            baos.write(byteChunk, 0, n);
          }
        }
        catch (IOException e)
        {
          e.printStackTrace ();
        }

        String s = new String(baos.toByteArray());
        Pattern p = Pattern.compile("href=\"(.*?)\"");
        Matcher m = p.matcher(s);
        String skin_url = "";
        if (m.find())
        {
            skin_url = m.group(1);
        }
        else
        {
            return url;
        }

        return skin_url;
    }

    @Override
    public ISkin getSkin(IPlayerProfile profile)
    {
        SkinData skin = new SkinData();
        if (_filter != null)
            skin.setSkinFilter(_filter);
        SharedPool.execute(() -> {
            if (_host != null && !_host.isEmpty())
            {
                String url = replaceValues(_host, profile);
                String skin_url = getSkinUrlFromUrl(url);

                if (!_host.equals(skin_url))
                {
                    Shared.downloadSkin(skin_url, Runnable::run).thenApply(Optional::get).thenAccept(data -> {
                        if (SkinData.validateData(data))
                            skin.put(data, SkinData.judgeSkinType(data));
                    });
                }
            }
        });
        return skin;
    }

    private String replaceValues(String host, IPlayerProfile profile)
    {
        return replaceValues(host, profile, "%name%", "%uuid%", "%auto%");
    }

    private String replaceValues(String host, IPlayerProfile profile, String nameKey, String uuidKey, String autoKey)
    {
        UUID id = profile.getPlayerID();
        String name = profile.getPlayerName();
        boolean isOffline = Shared.isOfflinePlayer(id, name);
        return host.replace(nameKey, name).replace(uuidKey, id.toString()).replace(autoKey, isOffline ? name : id.toString());
    }

    public CustomServerSkinProvider2 setHost(String host)
    {
        _host = host;
        return this;
    }

    public CustomServerSkinProvider2 withFilter(Function<ByteBuffer, ByteBuffer> filter)
    {
        _filter = filter;
        return this;
    }

}
