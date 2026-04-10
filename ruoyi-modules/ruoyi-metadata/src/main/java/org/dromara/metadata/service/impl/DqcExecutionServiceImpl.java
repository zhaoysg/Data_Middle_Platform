package org.dromara.metadata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.metadata.domain.DqcExecution;
import org.dromara.metadata.domain.DqcExecutionDetail;
import org.dromara.metadata.domain.DqcPlan;
import org.dromara.metadata.domain.DqcPlanRule;
import org.dromara.metadata.domain.DqcRuleDef;
import org.dromara.metadata.domain.MetadataColumn;
import org.dromara.metadata.domain.MetadataTable;
import org.dromara.metadata.domain.vo.DqcExecutionDetailVo;
import org.dromara.metadata.domain.vo.DqcExecutionVo;
import org.dromara.metadata.engine.executor.*;
import org.dromara.metadata.mapper.DqcExecutionDetailMapper;
import org.dromara.metadata.mapper.DqcExecutionMapper;
import org.dromara.metadata.mapper.DqcPlanMapper;
import org.dromara.metadata.mapper.DqcPlanRuleMapper;
import org.dromara.metadata.mapper.DqcRuleDefMapper;
import org.dromara.metadata.mapper.MetadataColumnMapper;
import org.dromara.metadata.mapper.MetadataTableMapper;
import org.dromara.metadata.service.IDqcExecutionService;
import org.dromara.metadata.service.IDqcQualityScoreService;
import org.dromara.metadata.support.DatasourceHelper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 闂傚倸鍊峰ù鍥ь浖閵娾晜鍤勯柤绋跨仛濞呯姵淇婇妶鍌氫壕闂佷紮绲介悘姘跺箯閸涘瓨鍊绘俊顖欒娴兼洟姊绘笟鈧褑澧濋梺鍝勬噺缁嬫垿鍩㈠澶娢у璺侯儑閸樼數绱撴担璇℃畷缂佸鍨佃灋妞ゆ挶鍨圭紒鈺伱归悩宸剱闁绘挻鐩弻娑㈩敃閿濆洨鐣奸悗娑欑箖缁绘繂鈻撻崹顔界亾闂佸摜濮甸幑鍥春閳ь剚銇勯幒鍡椾壕濠电姭鍋撻梺顒€绉寸壕濠氭煟閺冨洤浜圭€?- 闂傚倸鍊风粈渚€骞栭銈囩煋闁割偅娲嶉埀顒婄畵瀹曞ジ濮€閵忋垹顦╃紓鍌氬€哥粔鐢垫閺冪敻姊婚崒娆戭槮婵犫偓闁秵鍋￠柟鎯版楠炪垽鏌嶉崫鍕偓褰掑级閹间焦鐓熼幖娣焺閸熷繘鏌涢悩鎰佹當閻撱倖鎱ㄥ璇蹭壕濡?
 * <p>
 * 闂傚倸鍊烽懗鍫曗€﹂崼銏″床闁圭増婢橀悿顔姐亜閺嶎偄浠滄慨瑙勭叀閺岋綁寮崒姘粯缂備胶濮村鍫曞Φ閸曨垰鍗虫俊銈傚亾濞存粓绠栧鍝勭暦閸ャ劌娈岀紓浣割槺閹虫捇鎮惧畡鎵虫斀閻庯綆鈧厸鏅滈妵鍕敇閻旈浠撮梺鍝勬媼閸欏啴寮婚敐澶嬪亹閺夊牜鍋掗崬鐟扳攽閳╁啫绲婚悗姘煎枛鍗?
 * <ul>
 *   <li>闂傚倸鍊风粈浣革耿闁秵鍋￠柟鎯版楠炪垽鏌嶉崫鍕偓褰掑级閹间焦鈷掑ù锝呮啞閹牓鏌涢悤浣镐喊闁诡喓鍎茬缓鐣岀矙鐠恒劌濮︽俊鐐€栫敮濠傤渻閹烘挾顩查柕蹇嬪灮绾?tableId/columnId/compareTableId/compareColumnId 闂傚倸鍊风粈渚€骞栭銈嗗仏妞ゆ劧绠戠壕鍧楁煙缂併垹娅橀柡浣割儐娣囧﹪濡堕崨顔兼缂備讲鍋撻柛宀€鍋為悡鏇㈡煙閼割剙濡芥繛鍛缁绘盯宕煎┑鍫濈厽闂?/li>
 *   <li>闂傚倸鍊风粈渚€骞栭锔绘晞闁告侗鍨崑鎾愁潩閻撳骸顫紓?MetadataContext 濠电姷鏁搁崑鐔妓夐幇鏉跨；闁归偊鍘剧粻楣冩煃瑜滈崜鐔煎蓟瀹ュ牜妾ㄩ梺鍛婃尰濮樸劑寮鈧幃娆撴濞戞浜板┑鐐存綑閸氬顭囧▎鎴犱笉闁靛／鍕瀾闂佺鎻拹鐔煎焵椤戣法绐旀鐐差儔閺佸啴鍩€椤掑嫭鈷?/li>
 *   <li>濠电姷鏁搁崑鐐哄垂閸洖绠伴柛婵勫劤閻捇鏌熺紒銏犳灈闁汇値鍣ｉ弻宥堫檨闁告挾鍠栧濠氭晲婢跺﹦鍘告繛杈剧悼閸庛倝骞夋總鍛婄厽闁靛繆鏅涢悘鐘炽亜閺囥劌寮鐐茬箻閹晠宕归銏㈠酱闂備礁婀辩划顖滄暜閻愬搫绠?DqcRuleDef 濠电姷鏁搁崑鐐哄垂閸洖绠归柍鍝勬噹閸屻劑鏌熼鍡忓亾闁哄妫冮弻鐔衡偓鐢殿焾琚ラ梺绋款儐閹告悂鍩ユ径濞炬瀻婵☆垵宕甸弳顓熶繆閻愵亜鈧牠宕归幎鑺ュ€块柨鏂垮⒔閻瑥顭块懜闈涘缂佺姷鏁婚弻鐔兼倻濡儵鎷归梺纭呭蔼椤曆囧煘?/li>
 * </ul>
 * <p>
 * 闂傚倷娴囬褍霉閻戣棄绠犻柟鎯у殺閸ヮ剦鏁嶉柣鎰皺閻撴垿妫呴銏″婵﹤顭烽崺鐐差吋閸℃劒绨婚梺鍝勫暙閸婄敻骞忛埄鍐х箚?
 * <ul>
 *   <li>S4: 闂傚倸鍊风粈渚€骞夐敓鐘茬闁哄洢鍨圭粻鐘诲箹濞ｎ剙濡介柛瀣ㄥ€濋弻鏇熺箾閻愵剚鐝曢梺缁樻尪閸庨亶婀侀梺鎸庣箓閻楁粌顭囬幇顔剧＜闁稿本姘ㄩ悞鎼佹煛瀹€鈧崰鎾诲箯閻樿鐒洪柛鎰典簽閻涖垽姊?Redis 闂傚倸鍊风粈渚€骞夐敍鍕殰闁圭儤鍤﹀☉妯滄棃宕橀鍕Е闁诲海鎳撶€氫即宕戞繝鍥у惞闁哄洢鍨婚崣鎾绘煕閵夛絽濡块柍顖涙礋閺屸剝鎷呴悷鏉挎懙闂佸搫琚崝宀勫煡婢跺á鐔兼偂鎼存ê褰氱紓鍌氬€烽懗鍓佸垝椤栫偐鈧箓宕堕埡浣哥亰濠电偛妫欓崹鐔煎磻閹剧粯鏅查幖绮瑰墲閻忓牆鈹戦悙鍙夊櫡闁搞劋绮欏璇测槈濮楀棙鍍甸梺缁樻尭濞撮攱绂掕缁辨挻鎷呯拠鈩冪暥濡炪倧闄勬竟鍡欏垝濞嗘劕绶為柟閭﹀墻濞煎﹪姊虹紒姗堜緵闁哥姵娲熷鍫曨敇閵忊檧鎷洪梺鍦焾濞寸兘鍩ユ径鎰厸闁割偒鍋勬晶瀵糕偓娈垮櫘閸嬪﹪銆佸璺虹劦妞ゆ帒瀚畵渚€鏌熼柇锕€骞橀柣鐔风秺閺屽秷顧侀柛鎾寸懇閸┿垽寮崼鐔告珕闂佸憡鎸嗛崪浣剐為梻鍌欑窔濞佳囨偋閸℃瑦宕查柟鐑樻⒒閻?/li>
 *   <li>S3: 闂傚倸鍊烽悞锕€顪冮幐搴ｎ洸闁绘劕鎼粣妤呮煙娴兼潙浜伴柡浣告缁绘繃绻濋崒婊冾杸闂佺楠哥粔褰掑蓟濞戙垹鍗抽柕濞垮劚椤偊姊洪崨濠勬喛闁稿鎹囧娲嚒閵堝憛銏＄箾閺夋垵鈧悂鈥﹂崸妤€鐒垫い鎺戝閻撶娀鏌涢…鎴濅簻闁诲繐顕埀?闂?stopExecution 闂傚倷娴囧畷鍨叏瀹曞洨鐭嗗ù锝堫潐濞呯姴霉閻樺樊鍎愰柛瀣典邯閺屾盯鍩勯崘顏佹闂佹椿鍘介〃濠囧蓟閳ユ剚鍚嬮柛鎰╁妼椤鏌涢悢鍛婂€愭慨濠冩そ瀹曨偊宕熼澶堝灪缁绘稓浠﹂崒姘ｅ亾濡ゅ懎绠查柕蹇嬪€曠猾宥夋煃瑜滈崜鐔肩嵁韫囨稑宸濇い鎾楀啯鍊梻濠庡亜濞诧箑煤濠婂煻鍥敋閳ь剟寮婚敐澶嬪亜闁告縿鍎查崳褔鏌ｆ惔銏犲毈闁告挾鍠庨悾鐤亹閹烘繃鏅梺缁樺姌鐏忔瑩顢樻總鍛婂仭婵犲﹤瀚悘鎾煏閸℃洜绐旀鐐差儏閳规垿骞囬渚囧敨濠碉紕鍋戦崐鏇犳崲閹邦儵娑樜旈崘顏冪瑝濠殿喗顭堥崺鏍偂閺囩喓绠鹃柟瀵稿仧婢ь亜鈹戦檱婵倗鎹㈠┑瀣劦妞ゆ帒瀚弲鎼佹煟濡灝鐨洪柣锝変憾閹嘲顭ㄩ崘顔煎及闁芥鍠栭弻娑樷槈濞嗘劗绋囬梺?/li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DqcExecutionServiceImpl implements IDqcExecutionService {

    private final DqcExecutionMapper executionMapper;
    private final DqcExecutionDetailMapper detailMapper;
    private final DqcPlanMapper planMapper;
    private final DqcRuleDefMapper ruleDefMapper;
    private final DqcPlanRuleMapper planRuleMapper;
    private final DatasourceHelper datasourceHelper;
    private final RuleExecutorFactory executorFactory;
    private final IDqcQualityScoreService qualityScoreService;
    private final MetadataTableMapper metadataTableMapper;
    private final MetadataColumnMapper metadataColumnMapper;

    /** Redis 闂傚倸鍊搁崐椋庣矆娓氣偓婵￠潧螣閼测晝鐒兼繛杈剧秬椤鈻嶉悩缁樼厽婵☆垰鍚嬮弳鈺冪棯閸欍儳鐭欓柡灞界Х椤т線鏌涢幘璺烘灈鐎殿噮鍋勯濂稿椽娴ｈ銆冮梻浣筋嚙鐎涒晝绮╃粚宄筫c:lock:{planId} */
    private static final String LOCK_KEY_PREFIX = "dqcexec:lock:";
    /** Redis 闂傚倸鍊风粈渚€骞夐敓鐘冲仭闁挎洖鍊归崑瀣繆閵堝懎鏆熼柣顓熸崌閺岋綁骞嬮敐鍡╂闂佸憡鍔忛崑鎾绘⒒娴ｈ鍋犻柛搴㈢矒瀹曘劑顢涢敐鍛珒闂傚倸鍊风粈渚€骞夐敓鐘茬闁告縿鍎抽惌鎾绘煕閹捐尙鍔嶆い顐ｆ礋閺岀喐娼忔ィ鍐╊€嶉梺鍝勬媼閸撴岸骞堥妸銉㈡斀闁割偅鑹炬禍鐐箾閹惧啿鍔歺ec:cancel:{executionId} */
    private static final String CANCEL_KEY_PREFIX = "dqcexec:cancel:";
    /** 闂傚倸鍊搁崐椋庣矆娓氣偓婵￠潧顫滈埀顒勫箠濡ゅ懎閱囬柡鍥╁枎娴犻亶姊洪悷鎵憼缂佽绉瑰畷闈浳旈崨顔惧幈濠电偞鍨堕悷銉┧夌€ｎ兘鍋撳☉娆戠疄婵﹥妞藉畷顐﹀礋椤愶絾顔戞俊鐐€栧褰掑礉濡も偓鍗遍柟閭﹀厴閺嬪酣鏌熺€电啸闁绘挸顑夊铏规兜閸涱噮浠╅梺绋款儑閸犳挾鍒?*/
    private static final long LOCK_WAIT_SECONDS = 0;
    /** 闂傚倸鍊搁崐椋庣矆娓氣偓婵￠潧顫滈埀顒勫箖閻愮儤鏅滈梺娆惧灠娴滈箖鏌涢…鎴濅簼妞ゃ儱鐗忛埀顒侇問閸犳牠鎮樺顒夊殫闁告洦鍓氶崕鐔兼煥濞戞粌鐒介柛銊ょ矙瀵鏁撻悩鑼槰濡炪倖鎸嗛崘鈺傜彎缂傚倸鍊峰ù鍥ㄣ仈閹间焦鍋＄憸蹇涘礆閹烘绫嶉柛顐ｇ箘椤斿﹪姊虹紒妯哄闁轰焦鎮傝棟妞ゆ劧闄勯埛鎴︽煙缁嬫寧鎹ｉ柍钘夘樀閺岋綁顢橀悙娴嬪亾缁嬪灝绁梺鑽ゅТ濞层倕螣婵犲偆鐒藉ù鐓庣摠閻撳繘鐓崶褝鏀绘い顐躬閺屾盯寮拠鎻掑Е濠殿喖锕﹂崕銈咁嚗閸曨垰绠瑰ù锝堫嚃閳ь剚鐗犲娲焻閻愯尪瀚板褜鍣ｉ弻娑欑節娴ｈ櫣鐓撳銈冨灪閻熲晛顕ｉ幘顔藉亹闁汇垻顣槐瀛樹繆閻愵亜鈧牕顫忛悷鎷旓綁骞掗弮鍌滅劶?0闂傚倸鍊风粈渚€骞夐敍鍕殰闁圭儤鍤﹀☉妯锋斀閻庯綆浜為鍥⒑閻熸澘鈷旂紒顕呭灦瀵煡顢楅崒妤€浜鹃柛蹇擃槸娴滈箖姊洪崨濠傚闁告柨鐭傚绋库枎閹邦喚鐦堥梺闈涚箳婵挳銆傞幎鑺ョ厱闁绘娅曠粈瀣煏閸℃鍤囨い銏℃礋閺佸倿鎸婃径濠傤棆闂傚倷绀侀幖顐ゆ嫚閻愬瓨濯撮梺鍨儐瀹曡櫕绻濋悽闈浶為柛銊ャ偢瀹曟椽寮介銏＄€抽悗骞垮劚椤︻垰效閺屻儲鐓熼柟鎼幗閹差敥闂傚倷娴囧畷鐢稿窗閹扮増鍋￠柕澹偓閸嬫挸顫濋悡搴♀拫閻庤娲栫紞濠囥€佸☉銏″€烽柤纰卞墻閸氬鏌ｉ悢鍝ョ煁闁搞劌娼℃俊瀛樼瑹閳ь剟鐛€ｎ喗鏅濋柍褜鍓涢悮鎯ь吋婢跺鍘卞銈嗗姧缁插墽绮堥崘顔界叆婵炴垶鑹鹃弸娑㈡煛?*/
    private static final long LOCK_LEASE_SECONDS = 600;

    @Override
    public TableDataInfo<DqcExecutionVo> queryPageList(DqcExecutionVo vo, PageQuery pageQuery) {
        Wrapper<DqcExecution> wrapper = buildQueryWrapper(vo);
        var page = executionMapper.selectVoPage(pageQuery.build(), wrapper);
        for (DqcExecutionVo item : page.getRecords()) {
            formatVo(item);
        }
        return TableDataInfo.build(page);
    }

    @Override
    public DqcExecutionVo queryById(Long id) {
        DqcExecutionVo vo = executionMapper.selectVoById(id);
        if (vo != null) {
            formatVo(vo);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DqcExecution executePlan(Long planId, String triggerType, Long triggerUser) {
        RLock lock = getLock(planId);
        if (!tryLock(lock)) {
            throw new ServiceException("The plan is already running.");
        }

        DqcExecution execution = null;
        try {
            return doExecutePlan(planId, triggerType, triggerUser);
        } finally {
            unlock(lock);
        }
    }

    /**
     * 闂傚倷娴囬褎顨ョ粙鍖¤€块梺顒€绉寸壕濠氭煏閸繃濯奸柣搴ゅ煐閵囧嫰寮介顫捕缂備胶濮伴崕鎶藉箟閹间礁绠ｉ柨鏃囥€€閸嬫挻绗熼埀顒勭嵁鐎ｎ喗鏅濋柍褜鍓欒灋婵せ鍋撻柟顔煎槻閳诲氦绠涢幙鍐х礉闂備浇銆€閸嬫捇鏌熼幆鏉啃撻柣鎾卞劜缁绘盯骞嬮悜鍡欏姺婵犫拃鍕闁宠鍨块、娆撳箚瑜屽Σ鎰版⒑閸濆嫯顫﹂柛鏂跨Ч楠炲繘宕ㄩ弶鎴濈獩婵犵數濮撮崐鐢稿磻閹剧粯鍋ㄩ柛娑橈工娴狀垶鏌ｆ惔顖滅У濞存粍绮撳鎶芥偄閸忚偐鍘告繛杈剧秮濞佳囧焵椤掍胶绠炵€?
     */
    private DqcExecution doExecutePlan(Long planId, String triggerType, Long triggerUser) {
        DqcPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new ServiceException("闂傚倸鍊风粈渚€骞栭锕€纾瑰ù鐘差儜缂嶆牕顭块懜闈涘闁逞屽厸缁€渚€顢樻總绋垮耿婵炲棗鏈悘鍐煟鎼达紕鐣柛搴ㄤ憾楠炲﹪骞囬弶璺啈闂佺粯顭囩划顖炴偂? " + planId);
        }

        // 闂傚倸鍊风粈渚€骞夐敍鍕殰婵°倕鍟伴惌娆撴煙鐎电啸缁惧彞绮欓弻鐔煎箲閹伴潧娈紓浣哄О閸庢娊骞夐幖浣哥闁挎棁銆€閸嬫挻绗熼埀顒勭嵁鐎ｎ喗鍋愰柛鎰▕閳ь剚鐩娲传閸曞灚歇濠电偛顦板ú婊呭垝?
        DqcExecution execution = new DqcExecution();
        execution.setExecutionNo("DQC-" + System.currentTimeMillis());
        execution.setPlanId(planId);
        execution.setPlanName(plan.getPlanName());
        execution.setLayerCode(plan.getLayerCode());
        execution.setTriggerType(triggerType);
        execution.setTriggerUser(triggerUser);
        execution.setStartTime(LocalDateTime.now());
        execution.setStatus("RUNNING");
        executionMapper.insert(execution);

        // 闂傚倷娴囧畷鍨叏瀹曞洨鐭嗗ù锝堫潐濞呯姴霉閻樺樊鍎愰柛瀣典邯閺屾盯鍩勯崘顏佹闂佹椿鍘介〃濠囧蓟閳ユ剚鍚嬮柛鎰╁妼椤鏌涢悢鍛婂€愭慨濠冩そ瀹曨偊宕熼澶堝灪缁绘稓浠﹂崒姘ｅ亾濡ゅ懎绠查柕蹇嬪€曠猾宥夋煃瑜滈崜鐔肩嵁韫囨稑宸濇い鎾楀啯鍊梻濠庡亜濞诧箑煤濠婂煻鍥煛閸涱喒鎷哄┑鐐跺皺缁垱绻涢崶顒佺厱闁哄倹顑欓崕鎴︽煠?stopExecution 闂傚倸鍊烽懗鍫曗€﹂崼銉晞闁糕剝鐟ラ崹婵堚偓骞垮劚椤︿粙寮崱妯肩闁瑰瓨鐟ラ悘顏堟煛?
        String cancelKey = CANCEL_KEY_PREFIX + execution.getId();
        RedisUtils.setCacheObject(cancelKey, "0", Duration.ofHours(1));

        // 闂傚倸鍊风粈渚€骞栭銈嗗仏妞ゆ劧绠戠壕鍧楁煙缂併垹娅橀柡浣割儐娣囧﹪濡堕崨顓熸缂佺偓鍎抽妶鎼佸蓟閻旇　鍋撳☉娆樼劷缂佺姵锕㈤弻锟犲幢閺囩偛绁梺璇″枛閸㈡煡鍩㈡惔銈囩杸闁瑰灝鍟╅幃锝夋⒒?
        var planRules = planRuleMapper.selectList(
            Wrappers.<DqcPlanRule>lambdaQuery()
                .eq(DqcPlanRule::getPlanId, planId)
                .orderByAsc(DqcPlanRule::getSortOrder)
        );

        List<Long> ruleIds = planRules.stream().map(DqcPlanRule::getRuleId).toList();

        if (ruleIds.isEmpty()) {
            execution.setStatus("SUCCESS");
            execution.setTotalRules(0);
            execution.setPassedCount(0);
            execution.setFailedCount(0);
            execution.setBlockedCount(0);
            execution.setOverallScore(BigDecimal.valueOf(100));
            execution.setEndTime(LocalDateTime.now());
            execution.setElapsedMs(Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
            executionMapper.updateById(execution);
            return execution;
        }

        List<DqcRuleDef> rules = ruleDefMapper.selectCompatibleBatchIds(ruleIds);
        execution.setTotalRules(rules.size());

        int passed = 0, failed = 0, blocked = 0;
        Long execId = execution.getId();

        for (DqcRuleDef rule : rules) {
            // 婵犵數濮甸鏍闯椤栨粌绶ら柣锝呮湰瀹曟煡鏌熸潏鎯х槣闁轰礁鍟撮弻銈吤圭€ｎ偅鐝曢梺鍝ュТ濡繈寮诲☉銏犲嵆闁靛鍎扮花濠氭⒑鐠囪尙绠抽柛鐘崇墵瀵鎮㈤搹鍦暥濠电偞鍨惰摫闁挎稑绉归弻锟犲焵椤掍胶顩烽悗锝庡亞閸橀亶姊虹憴鍕姢缁剧虎鍘介悧搴繆閻愵亜鈧垿宕曢柆宥呯疇婵☆垯璀﹂崵鏇炩攽閻樺疇澹橀崬顖炴⒑閹稿孩鐓ョ憸鑸姂閺佸啴宕掑☉姘箞婵犳鍠楅敋濠⒀傜矙閹箖鎼归鐘辩盎濡炪倖鍔﹂崜娑㈡偩閻㈢鍋撶憴鍕┛缂傚秳绀侀锝嗙鐎ｎ€晜銇?
            if (isCancelled(execId)) {
                log.info("闂傚倸鍊风粈浣革耿闁秵鍋￠柟鎯版楠炪垽鏌嶉崫鍕偓褰掑级閹间焦鐓熼幖娣€ゅ鎰版煟閳╁啯绀堝ù婊勬倐椤㈡棃宕奸鍌溾棨闂備焦瀵уú鏍磹閹间焦鍎楁俊銈呮噺閻撴稓鈧箍鍎遍崯顐ｄ繆閸ф鐤? executionId={}, ruleId={}", execId, rule.getId());
                break;
            }

            DqcExecutionDetail detail = createExecutionDetail(execution, rule);

            try {
                // ========== 闂傚倸鍊烽懗鍫曗€﹂崼銏″床闁圭増婢橀悿顔姐亜閺嶎偄浠滄慨瑙勭叀閺岋綁寮崒姘粯缂備胶濮村鍫曞Φ閸曨垰鍗虫俊銈傚亾濞存粓绠栧鍝勭暦閸ャ劌娈岀紓浣割槺閹虫捇鎮惧畡鎵虫斀閻庯急鈧崑鎾绘晝閸屾氨顔婇梺鍝勫暙閸婅鐣烽崼鏇熲拻闁稿本鐟х拹浼存煕鐎ｎ亝鍣虹紒宀勪憾閹煎湱鎲撮崟顐㈢哎?tableId 闂傚倸鍊风粈渚€骞栭锔绘晞闁告侗鍨崑鎾愁潩閻撳骸顫紓?MetadataContext ==========
                MetadataContext context = buildMetadataContext(rule);
                DataSourceAdapter adapter = datasourceHelper.getAdapter(context.getDsId());

                RuleExecutor executor = executorFactory.getExecutor(rule.getRuleType());
                if (executor == null) {
                    log.warn("闂傚倸鍊风粈渚€骞栭锔藉亱婵犲﹤瀚々鍙夌節闂堟稒宸濋柣婵婂煐閹便劌顪冪拠韫闁诲氦顫夊ú妯虹暆閹间胶宓侀煫鍥ㄦ尵閺嗗棗鈹戦悩鎻掓殶缂佸鍏樺缁樻媴閸涘﹤鏆堥柣銏╁灡椤ㄥ﹪骞冮悿顖ｆЬ闂佽鍣崳锝夈€侀弴銏℃櫇闁逞屽墴瀹曞綊宕掗悙鏉戔偓鐢告煥濠靛棝顎楀褜鍨堕弻锝夋晲婢跺鍠氶梺鍝勭灱閸犳挾鍒掗鐐╂婵☆垯绱槐鐐烘⒑鐞涒€充壕婵炲濮撮鍡涙偂? ruleType={}, ruleId={}", rule.getRuleType(), rule.getId());
                    detail.setPassFlag("0");
                    detail.setErrorLevel(rule.getErrorLevel());
                    detail.setErrorMsg("闂傚倸鍊风粈渚€骞栭锔藉亱婵犲﹤瀚々鍙夌節闂堟稒宸濋柣婵婂煐閹便劌顪冪拠韫闁诲氦顫夊ú妯虹暆閹间胶宓侀煫鍥ㄦ尵閺嗗棗鈹戦悩鎻掓殶缂佸鍏樺缁樻媴閸涘﹤鏆堥柣銏╁灡椤ㄥ﹪骞冮悿顖ｆЬ闂佽鍣崳锝夈€侀弴銏℃櫇闁逞屽墴瀹曞綊宕掗悙鏉戔偓鐢告煥濠靛棝顎楀褜鍨堕弻锝夋晲婢跺鍠氶梺鍝勭灱閸犳挾鍒掗鐐╂婵☆垯绱槐鐐烘⒑鐞涒€充壕婵炲濮撮鍡涙偂? " + rule.getRuleType());
                    failed++;
                } else {
                    // 闂傚倸鍊搁崐鐑芥倿閿曚降浜归柛鎰典簽閻捇鏌涢锝嗙闁稿浜弻娑㈠焺閸愵亖妲堥梺娲诲幗椤ㄥ﹪寮婚垾鎰佸悑闁告劑鍔岄‖瀣煕閻斿憡鍊愰柡宀嬬秮閹晜娼忛顐㈡暭闂備礁鎲￠幐鑽ゆ崲閸繍鍤曢柟鎯板Г閸嬪嫰鏌ｉ幘鎶藉弰婵☆偆鍏樺娲濞戣京鍔搁梺绋块椤曨參鍩€椤掍胶顣查柣鐔叉櫊瀵鎮㈤搹鍦暥濠电偞鍨惰摫闁挎稑绉归弻锟犲焵椤掍胶顩烽悗锝庡亞閸欏棗鈹戦悙鏉戠仸妞ゎ厼娲﹂弲鍫曞籍閸屾粎锛滈梺鍛婎殘閸嬬喖宕洪敐鍥ｅ亾鐟欏嫭绀冩い銊ワ攻娣囧﹪鎮滈挊澹┿劑鏌曟径鍫濆姢闁荤喐鐓″缁樻媴閼恒儯鈧啴鏌ょ€圭姴鐓愰柡鍛版硾铻栭柍褜鍓熼幃楣冨垂椤愶絽顎撻梺闈╁瘜閸樻悂寮搁弽銊х瘈闁汇垽娼у皬闂佺厧鍟块悥濂稿箖閻戣棄绠荤紓浣诡焽閸樻悂姊虹粙鎸庢拱缁炬澘绉归獮鏍箛閻楀牏鍘甸梺鐐壘閸婄鐣甸崱娆屽亾鐟欏嫭绌跨紓宥勭椤曪絾绂掔€ｎ€晜銇勯澶婃灁闁稿骸寮剁粚杈ㄧ節閸ヨ埖鏅濋梺缁樻濞撹绔熼弴銏♀拻濞达絽鎲￠幆鍫ユ偠濮樼厧浜扮€规洘娲熷畷锟犳倷閳哄倻鈧椽姊洪崷顓炲妺妞ゃ劌妫濆?
                    if (executor instanceof AbstractRuleExecutor abstractExecutor) {
                        abstractExecutor.resetCancelCounter();
                    }
                    executor.execute(rule, detail, adapter, context, () -> isCancelled(execId));

                    if ("1".equals(detail.getPassFlag())) {
                        passed++;
                    } else {
                        failed++;
                        if ("HIGH".equals(detail.getErrorLevel()) || "CRITICAL".equals(detail.getErrorLevel())) {
                            blocked++;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("闂傚倷娴囧畷鐢稿窗閹扮増鍋￠柕澹偓閸嬫挸顫濋悡搴♀拫閻?{} 闂傚倸鍊风粈浣革耿闁秵鍋￠柟鎯版楠炪垽鏌嶉崫鍕偓褰掑级缁嬪簱鏀介柣姗嗗枛閻忣亪鏌涙惔銈呬汗闁告帗甯″畷婊嗩槼闁? {}", rule.getRuleName(), e.getMessage());
                detail.setPassFlag("0");
                detail.setErrorMsg(e.getMessage());
                detail.setErrorLevel(rule.getErrorLevel());
                if ("HIGH".equals(rule.getErrorLevel()) || "CRITICAL".equals(rule.getErrorLevel())) {
                    blocked++;
                }
                failed++;
            }

            detailMapper.insert(detail);
        }

        // 婵犵數濮烽弫鎼佸磻閻愬搫绠伴柟闂寸缁犵娀鏌熼悧鍫熺凡缂佺姵鐗曢埞鎴︽偐閸欏鎮欓梺娲诲幗椤ㄥ﹪寮婚垾鎰佸悑闁告劑鍔岄‖瀣煕閻斿憡鍊愭慨濠冩そ瀹曨偊宕熼澶堝灪缁绘稓浠﹂崒姘ｅ亾濡ゅ懎绠查柕蹇嬪€曠猾宥夋煃瑜滈崜鐔肩嵁?
        RedisUtils.deleteObject(cancelKey);

        BigDecimal score = rules.isEmpty() ? BigDecimal.valueOf(100)
            : BigDecimal.valueOf(passed * 100.0 / rules.size());

        String finalStatus;
        if (isCancelled(execution.getId())) {
            finalStatus = "STOPPED";
        } else if (failed == 0) {
            finalStatus = "SUCCESS";
        } else if (passed == 0) {
            finalStatus = "FAILED";
        } else {
            finalStatus = "PARTIAL";
        }

        execution.setPassedCount(passed);
        execution.setFailedCount(failed);
        execution.setBlockedCount(blocked);
        execution.setOverallScore(score);
        execution.setEndTime(LocalDateTime.now());
        execution.setElapsedMs(Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
        execution.setStatus(finalStatus);
        executionMapper.updateById(execution);

        plan.setLastExecutionId(execution.getId());
        plan.setLastScore(score);
        plan.setLastExecutionTime(execution.getEndTime());
        planMapper.updateById(plan);

        try {
            qualityScoreService.calculateAndSaveScore(execution.getId());
        } catch (Exception e) {
            log.error("闂傚倷娴囧畷鍨叏瀹曞洦顐介柕鍫濇处椤洟鏌￠崶銉ョ仾闁稿鏅犻弻銈嗘叏閹邦兘鍋撻弽顐熷亾濮橆剦鐓奸柡灞炬礃缁绘盯鎮欓浣哄絽婵犵數鍋涢幊宀勫磹濠靛绠栫憸鐗堝笒閻愬﹦鎲稿澶樻晜闁冲搫鎳忛悡鏇㈡煏婵炲灝鍔氭い鎺嬪灮閳ь剙鐏氬妯尖偓姘煎幖椤洩绠涘☉杈ㄦ櫇闂? executionId={}, error={}", execution.getId(), e.getMessage());
        }

        return execution;
    }

    /**
     * 闂傚倸鍊风粈渚€骞栭锔绘晞闁告侗鍨崑鎾愁潩閻撳骸顫紓浣介哺閹瑰洭鐛Ο鍏煎珰闁肩⒈鍓﹀Σ顖炴⒒娴ｅ憡鍟為柛鏂跨箻瀵彃顭ㄩ崘锝嗙亖闂佸壊鍋呭ú姗€鎮″▎鎾村仯闁搞儻绲洪崑鎾绘惞椤愩倓绨界紓鍌氬€烽悞锕傘€冮崨鏉戠９闁革富鍘鹃惌娆忣熆鐠哄搫顦柛瀣崌閺佹劖鎯旈垾鑼嚬闂?
     * <p>
     * 闂傚倸鍊搁崐椋庢閿熺姴纾婚柛娑卞枤閳瑰秹鏌ц箛姘兼綈鐎?rule 闂?tableId闂傚倸鍊风欢姘焽瑜嶈灋闁哄啫鐗嗙粻鎺楁煟閻樻鐓秛mnId闂傚倸鍊风欢姘焽瑜嶈灋闁哄啫鐗嗙粻鎺楁煟閻樼偣鈧檵areTableId闂傚倸鍊风欢姘焽瑜嶈灋闁哄啫鐗嗙粻鎺楁煟閻樼偣鈧檵areColumnId
     * 闂傚倸鍊风粈渚€骞栭銈嗗仏妞ゆ劧绠戠壕鍧楁煙缂併垹娅橀柡浣割儐娣囧﹪濡堕崨顔兼缂備讲鍋撻柛宀€鍋為悡鏇㈡煙閼割剙濡芥繛鍛缁绘盯宕煎┑鍫濈厽闂佸搫鐬奸崰鎾诲箯閻樿鐏抽柧蹇ｅ亞娴犲本淇婇妶鍥ラ柛瀣〒閹广垹螣閾忚娈鹃梺鎸庣箓椤︻垶宕橀埀顒勬偡濠婂啰绠荤€殿喛鍩栫粋鎺斺偓锝庡亐閹峰姊洪崨濠冨闁稿鎳樺畷鎶芥嚍閵壯咁啎闂佸壊鍋嗛崰搴ㄦ倶閳哄懏鐓欐い鏃囶嚙瀹撳棗鈹戦埄鍐╁€愬┑锛勫厴閺佸啴鍩€椤掑嫬鐒垫い鎺嶇閳绘洟鏌＄仦鐐鐎规洟浜跺鎾偑閳ь剙危椤曗偓濮婃椽鎮滈埡渚囨綒闂佸憡鍔х徊楣冩倶娓氣偓濮婂宕掑鍗烆杸缂備礁顑嗙敮鈥崇暦閹邦剛鏆﹂柛銉㈡櫇閿涙粌鈹戦悙鏉戠仸闁挎洍鏅涘嵄鐎瑰嫭澧ユ惔銊ョ倞鐟滄繈鐓渚囨?
     */
    private MetadataContext buildMetadataContext(DqcRuleDef rule) {
        String tableName = null;
        String columnName = null;
        String compareTableName = null;
        String compareColumnName = null;
        Long dsId = null;
        String dsCode = null;

        // 闂傚倸鍊风粈渚€骞栭銈嗗仏妞ゆ劧绠戠壕鍧楁煙缂併垹娅橀柡浣割儐娣囧﹪濡堕崨顔兼闂佺楠哥€涒晠濡甸崟顖氱睄闁稿本绋掗悵顏堟⒑閹肩偛濡奸柕鍫熸倐楠炲啰鎹勭悰鈩冾潔闂佸搫璇為崘鍓р偓鎶芥⒒娴ｅ憡鍟為拑閬嶆煙閻熺増鎼愭い鏇秮椤㈡洟鏁傞挊澶夌綍闂備礁澹婇崑鍛崲閸曨垰围?
        if (rule.getTableId() != null) {
            MetadataTable table = metadataTableMapper.selectById(rule.getTableId());
            if (table != null) {
                tableName = table.getTableName();
                dsId = table.getDsId();
                dsCode = table.getDsCode();
            }
        }

        // 闂傚倸鍊风粈渚€骞栭銈嗗仏妞ゆ劧绠戠壕鍧楁煙缂併垹娅橀柡浣割儐娣囧﹪濡堕崨顔兼闂佺楠哥€涒晠濡甸崟顖氱睄闁稿本绋掗悵顏堟⒑閹肩偛濡奸柕鍫熸倐楠炲啫螖閳ь剟鍩ユ径濞炬瀻婵☆垵宕甸弳锔戒繆閻愵亜鈧牠寮婚妸鈺傚剹闁稿本姘ㄩ弳锔戒繆閵堝懏鍣圭紒鈧崘顔界厓闁告繂瀚埀顒€缍婇、娆忣吋婢跺鎷?
        if (rule.getColumnId() != null) {
            MetadataColumn column = metadataColumnMapper.selectById(rule.getColumnId());
            if (column != null) {
                columnName = column.getColumnName();
            }
        }

        // 闂傚倸鍊风粈渚€骞栭銈嗗仏妞ゆ劧绠戠壕鍧楁煙缂併垹娅橀柡浣割儐娣囧﹪濡堕崨顓熸疁闂佺顑嗛幐濠氬箯閸涙潙绀冮柣鎰靛墰缁夌兘姊绘担椋庝覆閻庨潧鐭傚畷鏉款潩鐠鸿櫣鐣烘繛鏉戝悑濞兼瑦鍎梻渚€娼чˇ顓㈠磿鏉堚晝绠旀慨妯垮煐閳锋垿寮堕悙鏉戭€滈柛蹇撹嫰椤儻顦虫い銊ユ嚇閹?
        if (rule.getCompareTableId() != null) {
            MetadataTable compareTable = metadataTableMapper.selectById(rule.getCompareTableId());
            if (compareTable != null) {
                compareTableName = compareTable.getTableName();
            }
        }

        // 闂傚倸鍊风粈渚€骞栭銈嗗仏妞ゆ劧绠戠壕鍧楁煙缂併垹娅橀柡浣割儐娣囧﹪濡堕崨顓熸疁闂佺顑嗛幐濠氬箯閸涙潙绀冮柣鎰靛墰缁夌兘姊绘担椋庝覆閻庨潧鐭傚畷鏉款潩鐠鸿櫣鍔﹀銈嗗笂閼冲爼鍩婇弴鐔翠簻闁靛鍎查ˉ銏°亜閵忥紕鎳冮柣锝嗙箞瀹曠喖顢曢妶搴⑿濋梻鍌欑閹诧繝宕濊箛娑樼柧婵犲﹤鍟犻弸鏃堟煕椤愶絾绀冮柣?
        if (rule.getCompareColumnId() != null) {
            MetadataColumn compareColumn = metadataColumnMapper.selectById(rule.getCompareColumnId());
            if (compareColumn != null) {
                compareColumnName = compareColumn.getColumnName();
            }
        }

        return new MetadataContext(tableName, columnName, compareTableName, compareColumnName, dsId, dsCode);
    }

    /**
     * 闂傚倸鍊风粈渚€骞夐敍鍕殰婵°倕鍟伴惌娆撴煙鐎电啸缁惧彞绮欓弻鐔煎箲閹伴潧娈紓浣哄О閸庢娊骞夐幖浣哥闁挎棁銆€閸嬫挻绗熼埀顒勭嵁鐎ｎ喗鏅濋柍褜鍓熷畷锝嗙節閸パ咁啇闁哄鐗嗘晶浠嬪箖婵傚憡鐓?
     */
    private DqcExecutionDetail createExecutionDetail(DqcExecution execution, DqcRuleDef rule) {
        DqcExecutionDetail detail = new DqcExecutionDetail();
        detail.setExecutionId(execution.getId());
        detail.setRuleId(rule.getId());
        detail.setRuleName(rule.getRuleName());
        detail.setRuleCode(rule.getRuleCode());
        detail.setRuleType(rule.getRuleType());
        detail.setDimension(rule.getDimensions());
        detail.setTargetDsId(rule.getTableId() != null ? metadataTableMapper.selectById(rule.getTableId()).getDsId() : null);

        // 闂傚倸鍊搁崐椋庢閿熺姴纾婚柛娑卞枤閳瑰秹鏌ц箛姘兼綈鐎规洘鐓￠弻娑㈠箛闂堟稒鐏堢紓浣插亾闁稿瞼鍋為悡鏇㈡煙閼割剙濡芥繛鍛缁绘盯宕煎┑鍫濈厽闂佸搫鐬奸崰鎾诲箯閻樿鐏抽柧蹇ｅ亞娴犲本淇婇妶鍥ラ柛瀣〒閹广垹螣閾忚娈鹃梺鎸庣箓椤︻垶宕橀埀顒€顪冮妶鍡楃伌闁轰緡鍣ｅ畷鎴﹀箻鐠囪尙顦ㄥ銈嗘婢瑰牐顤傞梺璇查缁犲秹宕曢柆宥呯疇闊洦娲嶉崑鎾愁潩椤撶姴寮ㄩ梺璇″枟椤ㄥ懘鍩ユ径濞炬瀻婵☆垵宕甸弳锔戒繆閻愵亜鈧牠寮婚妸鈺傚剹闁稿本姘ㄩ弳?
        if (rule.getTableId() != null) {
            MetadataTable table = metadataTableMapper.selectById(rule.getTableId());
            if (table != null) {
                detail.setTargetTable(table.getTableName());
            }
        }
        if (rule.getColumnId() != null) {
            MetadataColumn column = metadataColumnMapper.selectById(rule.getColumnId());
            if (column != null) {
                detail.setTargetColumn(column.getColumnName());
            }
        }

        detail.setExecuteTime(LocalDateTime.now());
        return detail;
    }

    @Override
    public List<DqcExecutionDetailVo> listDetailsByExecutionId(Long executionId) {
        List<DqcExecutionDetailVo> details = detailMapper.selectVoList(
            Wrappers.<DqcExecutionDetail>lambdaQuery()
                .eq(DqcExecutionDetail::getExecutionId, executionId)
                .orderByAsc(DqcExecutionDetail::getId)
        );
        sanitizeCustomSqlDetails(details);
        return details;
    }

    @Override
    public List<DqcExecutionVo> listByPlanId(Long planId) {
        return executionMapper.selectVoList(
            Wrappers.<DqcExecution>lambdaQuery()
                .eq(DqcExecution::getPlanId, planId)
                .orderByDesc(DqcExecution::getCreateTime)
        );
    }

    /**
     * 闂傚倸鍊风粈渚€宕ョ€ｎ喖纾块柟鎯版鎼村﹪鏌ら懝鎵牚濞?Redis 闂傚倸鍊风粈渚€骞夐敍鍕殰闁圭儤鍤﹀☉妯滄棃宕橀鍕Е闁诲海鎳撶€氫即宕戞繝鍥у惞闁哄洢鍨婚崣鎾绘煕閵夛絽濡块柍顖涙礋閺?
     */
    private RLock getLock(Long planId) {
        return RedisUtils.getClient().getLock(LOCK_KEY_PREFIX + planId);
    }

    /**
     * 闂傚倷娴囬褏鎹㈤幇顔藉床闁瑰濮靛畷鏌ユ煕閳╁啰鈯曢柛搴★攻閵囧嫰寮介顫勃闂佹悶鍊栧濠氬焵椤掑倹鍤€閻庢凹鍘奸…鍨熼悡搴ｇ瓘闂佺鍕垫畷闁绘挻娲熼弻鐔兼倻濡櫣浠稿銈冨劜缁海妲愰幒妤€妫橀柛顭戝枟浜涙俊鐐€栧ú鈺冪礊閳ь剛绱掗悩宕団槈闁宠棄顦埢搴ㄥ箛椤撶喓鏆?
     */
    private boolean tryLock(RLock lock) {
        try {
            return lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("Interrupted while acquiring the execution lock.");
        }
    }

    /**
     * 闂傚倸鍊搁崐鐑芥倿閿曚降浜归柛鎰靛枟閺呮繈鏌曟径鍡樻珔婵☆偅锚閵嗘帒顫濋敐鍛闁诲氦顫夊ú妯虹暆缁嬭法鏆﹂柛妤冨€ｉ弮鍫濈闁告瑣鍎崇粈澶愭⒑鐠囨彃顒㈡い鏃€鐗犲畷浼村冀椤愶絽搴婂┑顔姐仜閸嬫挾鈧?
     */
    private void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 婵犵數濮烽。钘壩ｉ崨鏉戠；闁逞屽墴閺屾稓鈧綆鍋呭畷宀勬煛瀹€瀣？濞寸媴濡囬幏鐘诲箵閹烘埈娼ユ繝鐢靛Л閹峰啴宕ㄩ鐣屽涧闂備浇顕栭崰妤佺珶閸℃顩烽柨鏇炲€归崐閿嬨亜閹搭厼浜剧紒鐘虫崌瀵鈽夊鍡樺兊闂佸憡鎸嗛崨顔筋啅闂傚倷绀侀幉锟犲春婵犲嫭顫曢柡鍥ュ灩缁犳牗銇勯弴妤€浜惧銈冨灪濞茬喐鎱ㄩ埀顒勬煟濡櫣浠涢柛瀣斿嫮绡€?
     */
    private boolean isCancelled(Long executionId) {
        String cancelKey = CANCEL_KEY_PREFIX + executionId;
        String value = RedisUtils.getCacheObject(cancelKey);
        return "1".equals(value);
    }

    @Override
    public void stopExecution(Long executionId) {
        DqcExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw new ServiceException("Execution record does not exist.");
        }

        if (!"RUNNING".equals(execution.getStatus())) {
            throw new ServiceException("Only running executions can be stopped.");
        }

        // 闂傚倷娴囧畷鍨叏瀹曞洨鐭嗗ù锝堫潐濞呯姴霉閻樺樊鍎愰柛瀣典邯閺屾盯鍩勯崘顏佹闂佹椿鍘介〃濠囧蓟閳ユ剚鍚嬮柛鎰╁妼椤鏌涢悢鍛婂€愭慨濠冩そ瀹曨偊宕熼澶堝灪缁绘稓浠﹂崒姘ｅ亾濡ゅ懎绠查柕蹇嬪€曠猾宥夋煃瑜滈崜鐔肩嵁韫囨稑宸濇い鎾楀啯鍊梻濠庡亜濞诧箑煤濠婂煻鍥偨椤╂椆xecutePlan 闂傚倸鍊烽悞锕傛儑瑜版帒绀夌€光偓閳ь剟鍩€椤掍礁鍤柛銊ョ埣閺佹劙鎮欓弶鎴犵獮闂佸綊鍋婇崜娑⑺囬鈶╂斀闁绘劕寮堕ˉ婊呯磼缂佹ê鐏寸€规洘娲熷鎾閿涘嫬骞楁繝鐢靛Т閿曘倖顨ラ幖浣瑰€堕柣鏃囨绾惧ジ鏌涘▎鎯奉亜鐣甸崱娆屽亾鐟欏嫭纾搁柛鏃€鍨舵穱濠囧箹娴ｈ娅嗛梺鑺ッˇ閬嶅礉椤栫偞鈷掑ù锝囩摂閸ゅ啴鏌涙惔銏″€愮€规洜鍠栧畷褰掝敃閻旂鐦查梻鍌氬€风粈渚€骞栭銈嗗仏妞ゆ劧绠戠壕鍧楁煛鐏炶鍔撮柡浣告缁绘繈妫冨☉鍗炲壈闂佸憡鍔忛崑鎾绘⒒娴ｈ鍋犻柛搴㈢矒瀹曘劑顢涢敐鍛珒
        String cancelKey = CANCEL_KEY_PREFIX + executionId;
        RedisUtils.setCacheObject(cancelKey, "1", Duration.ofHours(1));

        // 闂傚倸鍊风粈渚€骞栭銈囩煋闁哄鍤氬ú顏勭厸闁告粈鐒﹂弲鈺呮⒑閹肩偛鍔撮柛鎾寸懅缁濡疯閸犳劙鏌熸潏鎹愮闁逞屽厸缁舵岸鐛€ｎ喗鍊风痪鐗埳戦悘鍐⒒娴ｅ憡鍟炵紒璇插€婚埀顒佸嚬娴滅偤宕氶幒鏃傜＜婵☆垵顕у鍨攽閻樼粯娑ч悗姘煎墴閹﹢顢欑亸鏍ㄦ杸濡炪倖妫佹慨銈夊磹閹邦収娈介柣鎰ㄦ櫅娴?
        execution.setStatus("STOPPED");
        execution.setEndTime(LocalDateTime.now());
        execution.setElapsedMs(Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
        executionMapper.updateById(execution);

        log.info("闂傚倸鍊风粈浣革耿闁秵鍋￠柟鎯版楠炪垽鏌嶉崫鍕偓褰掑级閹间焦鐓熼幖绮瑰墲鐠愨剝銇勯鐐靛妞ゆ洩缍佸畷濂稿即閻旇渹鍑介梻浣瑰缁诲倸煤閿旇棄鍨斿┑鍌氭啞閻撴稑顭跨捄渚劸濞寸姵甯炵槐鎺楊敊閸撗冪闂侀€涚┒閸斿矂鍩為幋锕€绠涙い鎾跺Х閻浹呯磽閸屾瑦绁板鏉戞憸閹广垽宕熼鐐茬亰闂佸壊鐓堥崑鍕閻愭祴鏀介柣妯诲絻椤忣偅銇勬惔妯轰壕闂傚倷娴囧畷鐢稿窗閹扮増鍋￠柕澹偓閸嬫挸顫濋悡搴♀拫閻庤娲栫紞濠囥€佸☉姗嗘僵闁绘挸瀛╅鍌涗繆閻愵亜鈧洜鎹㈤幇顑╂稑螖閸愵亙绗夊┑顔筋焾閸╂牠鍩涢幋鐐簻闁归偊鍠栭弸搴∶瑰鍫㈢暫婵? executionId={}", executionId);
    }

    @Override
    public DqcExecution rerunExecution(Long executionId, Long triggerUser) {
        DqcExecution oldExecution = executionMapper.selectById(executionId);
        if (oldExecution == null) {
            throw new ServiceException("Execution record does not exist.");
        }

        if (oldExecution.getPlanId() == null) {
            throw new ServiceException("Cannot rerun because the original execution is not linked to a plan.");
        }

        RLock lock = getLock(oldExecution.getPlanId());
        if (!tryLock(lock)) {
            throw new ServiceException("The plan is already running.");
        }

        try {
            return doExecutePlan(oldExecution.getPlanId(), "MANUAL", triggerUser);
        } finally {
            unlock(lock);
        }
    }

    /**
     * 闂傚倸鍊风粈渚€骞栭銈囩煋闁割偅娲栭崒銊ф喐韫囨拹锝夊箛閻楀牊娅㈤梺缁橆焾鐏忔瑩藝闁秵鈷戦柛婵嗗琚梺鍛婃煥閻倿宕哄☉銏犵睄闁割偆鍠庢禒鈺佲攽閻愭潙鐏︽い顓炴喘閹偤鏌ㄧ€ｃ劋绨婚梺鍝勫€圭€笛呯矚閸ф鐓忛柛銉ｅ妼婵秶鈧鍠曢崡鍐差嚕?
     */
    private void formatVo(DqcExecutionVo vo) {
        if (vo == null) return;
        if ("RUNNING".equals(vo.getStatus())) {
            vo.setStatusText("RUNNING");
        } else if ("SUCCESS".equals(vo.getStatus())) {
            vo.setStatusText("SUCCESS");
        } else if ("FAILED".equals(vo.getStatus())) {
            vo.setStatusText("FAILED");
        } else if ("PARTIAL".equals(vo.getStatus())) {
            vo.setStatusText("PARTIAL");
        } else if ("STOPPED".equals(vo.getStatus())) {
            vo.setStatusText("STOPPED");
        }
    }

    /**
     * 闂傚倸鍊风粈渚€骞栭锔绘晞闁告侗鍨崑鎾愁潩閻撳骸顫紓浣介哺閹瑰洭鐛Ο鍏煎珰闁艰壈鍩栭幉浼存⒒娓氣偓濞佳囁囨禒瀣亗闁割偁鍎遍崥褰掓煕閺囥劌鐏￠柣鎾寸☉椤法鎹勯悜姗嗘闂佺顑嗛幑鍥х暦閿濆棗绶炴俊顖濇〃缂?
     */
    private Wrapper<DqcExecution> buildQueryWrapper(DqcExecutionVo vo) {
        LambdaQueryWrapper<DqcExecution> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ObjectUtil.isNotNull(vo.getPlanId()), DqcExecution::getPlanId, vo.getPlanId())
            .like(StringUtils.isNotBlank(vo.getPlanName()), DqcExecution::getPlanName, vo.getPlanName())
            .eq(StringUtils.isNotBlank(vo.getStatus()), DqcExecution::getStatus, vo.getStatus())
            .eq(StringUtils.isNotBlank(vo.getTriggerType()), DqcExecution::getTriggerType, vo.getTriggerType())
            .orderByDesc(DqcExecution::getCreateTime);
        return wrapper;
    }

    private void sanitizeCustomSqlDetails(List<DqcExecutionDetailVo> details) {
        for (DqcExecutionDetailVo detail : details) {
            if (!CustomSqlExecutor.TYPE.equalsIgnoreCase(detail.getRuleType())) {
                continue;
            }
            detail.setExecuteSql(CustomSqlSecuritySupport.REDACTED_SQL);
            detail.setActualValue(null);
            if (StringUtils.isNotBlank(detail.getErrorMsg())) {
                detail.setErrorMsg(CustomSqlSecuritySupport.EXECUTION_ERROR);
            }
        }
    }
}

