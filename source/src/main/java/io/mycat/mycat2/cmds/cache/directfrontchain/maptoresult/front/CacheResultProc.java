package io.mycat.mycat2.cmds.cache.directfrontchain.maptoresult.front;

import java.nio.channels.SelectionKey;

import io.mycat.mycat2.MycatSession;
import io.mycat.mycat2.cmds.DirectPassthrouhCmd;
import io.mycat.mycat2.common.ChainExecInf;
import io.mycat.mycat2.common.SeqContextList;
import io.mycat.mycat2.console.SessionKeyEnum;
import io.mycat.proxy.ProxyBuffer;

/**
 * 缓存的结果集处理
 * 
 * @since 2017年9月22日 下午11:38:17
 * @version 0.0.1
 * @author liujun
 */
public class CacheResultProc implements ChainExecInf {

	/**
	 * 实例对象
	 */
	public static final CacheResultProc INSTANCE = new CacheResultProc();

	@Override
	public boolean invoke(SeqContextList seqList) throws Exception {

		MycatSession session = (MycatSession) seqList.getSession();

		ProxyBuffer curBuffer = session.proxyBuffer;

		Boolean check = (Boolean) session.curBackend.getSessionAttrMap()
				.get(SessionKeyEnum.SESSION_KEY_CONN_IDLE_FLAG.getKey());

		// 检查到当前已经完成,执行添加操作
		if (null != check && check) {
			curBuffer.flip();
			session.writeToChannel();
			// 当知道操作完成后，前段的注册感兴趣事件为读取
			session.takeOwner(SelectionKey.OP_READ);
			// 完成后，移除偏移标识
			session.getSessionAttrMap().remove(SessionKeyEnum.SESSION_KEY_GET_OFFSET_FLAG.getKey());

			// 当完成之后，切换回透传流程处理
			session.getCmdChain().setTarget(DirectPassthrouhCmd.INSTANCE);
		}
		// 未完成执行继续读取操作
		else {
			curBuffer.flip();
			// 前段获取事件
			session.takeOwner(SelectionKey.OP_WRITE);
			session.writeToChannel();
		}

		return seqList.nextExec();
	}

}
