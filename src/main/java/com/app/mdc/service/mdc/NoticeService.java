package com.app.mdc.service.mdc;

import com.app.mdc.model.mdc.FeedBack;
import com.app.mdc.model.mdc.Notice;
import com.baomidou.mybatisplus.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 
 * @since 2020-02-05
 */
public interface NoticeService extends IService<Notice> {

    /**
     * 查询最新公告
     * @return
     */
    Notice selectNewest();
}
