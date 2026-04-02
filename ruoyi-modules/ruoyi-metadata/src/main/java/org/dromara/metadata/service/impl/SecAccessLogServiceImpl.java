package org.dromara.metadata.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.SecAccessLog;
import org.dromara.metadata.domain.vo.SecAccessLogVo;
import org.dromara.metadata.mapper.SecAccessLogMapper;
import org.dromara.metadata.service.ISecAccessLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 脱敏访问日志服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class SecAccessLogServiceImpl implements ISecAccessLogService {

    private final SecAccessLogMapper baseMapper;

    @Override
    public TableDataInfo<SecAccessLogVo> queryPageList(SecAccessLogVo vo, PageQuery pageQuery) {
        LambdaQueryWrapper<SecAccessLog> wrapper = Wrappers.<SecAccessLog>lambdaQuery()
            .eq(vo.getDsId() != null, SecAccessLog::getDsId, vo.getDsId())
            .eq(vo.getUserId() != null, SecAccessLog::getUserId, vo.getUserId())
            .like(StringUtils.isNotBlank(vo.getUserName()), SecAccessLog::getUserName, vo.getUserName())
            .eq(StringUtils.isNotBlank(vo.getStatus()), SecAccessLog::getStatus, vo.getStatus())
            .ge(vo.getParams() != null && vo.getParams().get("beginTime") != null,
                SecAccessLog::getCreateTime, vo.getParams().get("beginTime"))
            .le(vo.getParams() != null && vo.getParams().get("endTime") != null,
                SecAccessLog::getCreateTime, vo.getParams().get("endTime"))
            .orderByDesc(SecAccessLog::getCreateTime);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public SecAccessLogVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    @Async
    public void logAsync(Long dsId, String sql, String maskedSql, int rows,
                          List<String> maskedColumns, long elapsedMs, String status, String errorMsg,
                          Long userId, String userName, String ipAddress) {
        try {
            SecAccessLog log = new SecAccessLog();
            log.setDsId(dsId);
            log.setQuerySql(sql);
            log.setMaskedSql(maskedSql);
            log.setResultRows(rows);
            log.setMaskedColumns(JSON.toJSONString(maskedColumns));
            log.setElapsedMs(elapsedMs);
            log.setStatus(status);
            log.setErrorMsg(errorMsg);
            log.setUserId(userId);
            log.setUserName(userName);
            log.setIpAddress(ipAddress);
            log.setCreateTime(new Date());
            baseMapper.insert(log);
        } catch (Exception e) {
            log.error("写入脱敏访问日志失败", e);
        }
    }

    @Override
    public List<SecAccessLogVo> queryByUserId(Long userId, int limit) {
        return baseMapper.selectVoList(
            Wrappers.<SecAccessLog>lambdaQuery()
                .eq(SecAccessLog::getUserId, userId)
                .orderByDesc(SecAccessLog::getCreateTime)
                .last("LIMIT " + limit)
        );
    }

    @Override
    public List<SecAccessLogVo> queryByDsId(Long dsId, int limit) {
        return baseMapper.selectVoList(
            Wrappers.<SecAccessLog>lambdaQuery()
                .eq(SecAccessLog::getDsId, dsId)
                .orderByDesc(SecAccessLog::getCreateTime)
                .last("LIMIT " + limit)
        );
    }
}
