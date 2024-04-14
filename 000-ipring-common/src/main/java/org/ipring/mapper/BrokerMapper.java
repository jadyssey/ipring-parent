package org.ipring.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.ipring.model.dobj.broker.BrokerDO;
import org.ipring.model.vo.broker.BrokerInfoVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author: Rainful
 * @date: 2024/04/02 10:57
 * @description:
 */
@Mapper
public interface BrokerMapper extends BaseMapper<BrokerDO> {

    /**
     * 查询交易商列表信息
     * @return
     */
    List<BrokerInfoVO> selectBrokerInfoList();

}
