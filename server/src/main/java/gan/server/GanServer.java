package gan.server;

import android.os.Looper;
import gan.core.file.SharePerference;
import gan.core.system.server.SystemServer;
import gan.log.DebugLog;
import gan.log.FileLogger;
import gan.media.MediaApplication;
import gan.media.ffmpeg.FfmpegMediaServerManager;
import gan.media.rtsp.RtspMediaServerManager;
import gan.server.config.FFmepg;
import gan.server.config.Gan;
import gan.web.config.MediaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

@SpringBootApplication(
        scanBasePackages = {"gan.web", "gan.server.config", "gan.server.web"},
        exclude = DataSourceAutoConfiguration.class)
public class GanServer extends MediaApplication {

    public static void main(String[] args) {
        Logger.getLogger(GanServer.class.getName()).info("main start");
        Thread.currentThread().setName("main");
        Looper.prepareMainLooper();
        try{
            ApplicationContext context = new SpringApplicationBuilder(GanServer.class)
                    .run(args);
            SystemServer.getInstance().create(context);
        }finally {
            Looper.loop();
        }
    }

    @Autowired
    private Gan gan;
    @Autowired
    FFmepg  ffmpeg;

    public static GanServer getInstance() {
        return (GanServer) sInstance;
    }

    @Override
    protected void onCreate(ApplicationContext context) {
        FileLogger.getInfoLogger().setLogcat(gan.debug);
        FileLogger.getDebugLogger().setLogcat(gan.debug);
        if(gan.debug){
            DebugLog.setLevel(gan.logLevel);
        }else{
            DebugLog.setLevel(DebugLog.INFO);
        }
        super.onCreate(context);
        MediaConfig config = getMediaConfig();
        if(config.rtspEnable){
            DebugLog.info( "rtsp_port:"+config.rtspPort);
            addManager(RtspMediaServerManager.getInstance());
            RtspMediaServerManager.getInstance().initServer();
        }

        if(gan.gb28181Enable){
        }

        if(gan.jt1078Enable){
        }

        try {
            this.getClass().getClassLoader().loadClass(FfmpegMediaServerManager.class.getName());
            addManager(FfmpegMediaServerManager.getInstance());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if(gan.rtmpEnable){
        }
    }

    public static Gan getGan() {
        return getInstance().gan;
    }

    public static FFmepg getFFmpeg() {
        return getInstance().ffmpeg;
    }

    public SharePerference getSharePerference() {
        return super.getSharePerference("gan");
    }

    public static String getPublicPath(String path){
        if(path.startsWith("/")){
            return getRootPath("/public"+path);
        }
        return getRootPath("/public/"+path);
    }

    public boolean isDebug(){
        return getGan().debug;
    }

}
