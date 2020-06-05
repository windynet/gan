package gan.server.web.controller;

import gan.web.base.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(value = "设备接口文档", tags = { "设备控制" })
public class XBaseController {

    @RequestMapping(value = "/live/start",method = {RequestMethod.POST,RequestMethod.GET})
    @ApiOperation("播放视频")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType="query",name = "model", value = "设备型号", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(paramType="query",name = "deviceId", value = "设备id", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(paramType="query",name = "channel", value = "设备通道", dataTypeClass = String.class, required = true)
    })
    @ResponseBody
    public Result openStream(){
        String url = "rtsp://xxx";
        return Result.ok().setData(url);
    }

    @RequestMapping(value = "/live/stop",method = {RequestMethod.POST,RequestMethod.GET})
    @ApiOperation("结束视频")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType="query",name = "deviceId", value = "设备id", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(paramType="query",name = "channel", value = "设备通道", dataTypeClass = String.class, required = true)
    })
    @ResponseBody
    public Result closeStream(){
        return Result.ok();
    }

    @RequestMapping(value = "/record/list",method = {RequestMethod.POST,RequestMethod.GET})
    @ApiOperation("查询时间段录像")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType="query",name = "deviceId", value = "设备id", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(paramType="query",name = "channel", value = "设备通道", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(paramType="query",name = "timeStart", value = "开始时间戳 单位秒", dataTypeClass = Long.class, required = true),
            @ApiImplicitParam(paramType="query",name = "timeEnd", value = "结束时间戳 单位秒", dataTypeClass = Long.class, required = true)
    })
    @ResponseBody
    public Result recordList(){
        return Result.ok();
    }

    @RequestMapping(value = "/record/start",method = {RequestMethod.POST,RequestMethod.GET})
    @ApiOperation("播放录像")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType="query",name = "deviceId", value = "设备id", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(paramType="query",name = "channel", value = "设备通道", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(paramType="query",name = "timeStart", value = "开始时间戳 单位秒", dataTypeClass = Long.class),
            @ApiImplicitParam(paramType="query",name = "timeEnd", value = "结束时间戳 单位秒", dataTypeClass = Long.class),
            @ApiImplicitParam(paramType="query",name = "filePath", value = "录像文件路径", dataTypeClass = Long.class),
    })
    @ResponseBody
    public Result recordStart(){
        return Result.ok();
    }

    @RequestMapping(value = "/record/start",method = {RequestMethod.POST,RequestMethod.GET})
    @ApiOperation("录像控制")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType="query",name = "deviceId", value = "设备id", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(paramType="query",name = "channel", value = "设备通道", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(paramType="query",name = "filePath", value = "录像文件路径", dataTypeClass = Long.class),
            @ApiImplicitParam(paramType="query",name = "position", value = "开始时间点 单位秒", dataTypeClass = Long.class),
            @ApiImplicitParam(paramType="query",name = "speed", value = "倍速", dataTypeClass = Long.class),
    })
    @ResponseBody
    public Result recordControl(){
        return Result.ok();
    }

    public Result recordEnd(){
        return Result.ok();
    }

    public Result recordDownload(){
        return Result.ok();
    }

    public Result ptzStart(){
        return Result.ok();
    }

    public Result ptzStop(){
        return Result.ok();
    }

    public Result onlineList(){
        return Result.ok();
    }

}
