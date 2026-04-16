package org.dromara.metadata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.DqcPlanRule;
import org.dromara.metadata.domain.DqcRuleDef;
import org.dromara.metadata.domain.MetadataColumn;
import org.dromara.metadata.domain.MetadataTable;
import org.dromara.metadata.domain.bo.DqcRuleDefBo;
import org.dromara.metadata.domain.vo.DqcRuleDefVo;
import org.dromara.metadata.engine.executor.CustomSqlExecutor;
import org.dromara.metadata.engine.executor.CustomSqlSecuritySupport;
import org.dromara.metadata.mapper.DqcPlanRuleMapper;
import org.dromara.metadata.mapper.DqcRuleDefMapper;
import org.dromara.metadata.mapper.MetadataColumnMapper;
import org.dromara.metadata.mapper.MetadataTableMapper;
import org.dromara.metadata.service.IDqcRuleDefService;
import org.dromara.metadata.support.DatasourceHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 闂傚倸鍊峰ù鍥ь浖閵娾晜鍤勯柤绋跨仛濞呯姵淇婇妶鍌氫壕闂佷紮绲介悘姘跺箯閸涘瓨鍊绘俊顖欒娴兼洟姊绘笟鈧褑澧濋梺鍝勬噺缁嬫垿鍩㈠澶娢у璺侯儏娴狀厼鈹戦悙鍙夘棞缂佺粯鍔欓、鏃堝煛閸涱喚鍘卞銈庡幗閸ㄧ敻鎮橀敐澶嬬厓鐟滄粓宕滃┑瀣剁稏濠㈣埖鍔栭崕妤併亜閺傚灝鈷斿☉鎾崇Ч閺岋綁寮崒姘粯闁诲孩鑹鹃ˇ閬嶅焵椤掆偓缁犲秹宕曢柆宥呯疇閹兼惌鐓夌紞鏍煏閸繍妲归柛濠勬暬閺屾盯鏁傜拠鎻掔婵炲濮嶉崟鍨啍?
 * <p>
 * 闂傚倸鍊搁崐鐑芥倿閿曚降浜归柛鎰电厑濞差亜惟闁宠桨鐒﹂悗顒勬⒑闂堟稓澧曟繛灞傚妼铻炴い鏍ㄧ◤娴滄粓鏌″鍐ㄥ濠㈣锕㈤幃浠嬵敍濞戣鲸鐤侀梺鍝勫閸撴繈骞忛崨鏉戜紶闁告洖鐏氬В澶愭煟鎼淬値娼愰柟鍝ュ厴閹偤鏁冮崒姣硷附绻涢崱妯诲碍缁绢厸鍋撻梻浣虹帛閸旀牠鎮鹃鈶芥棃宕ㄩ闂村寲濠电偠鎻徊鍧楀箠閹惧墎鐭嗛柛鎰ㄦ杺娴滄粓鏌熼幆褏鎽犻柛濠冨姍閺?
 * - 闂傚倸鍊烽懗鍫曞磿閻㈢鐤鹃柍鍝勬噹缁愭淇婇妶鍛櫤闁稿顦甸弻銊モ攽閸℃ê顦╅梺?闂傚倷娴囬褏鈧稈鏅濈划娆撳箳濡炲皷鍋撻崘顔煎耿婵炴垼椴搁弲鈺呮倵閸忓浜鹃梺鍛婃处閸撴艾鈻嶉弽褉鏀芥い鏃傜叓椤忓牊鍊堕柛顐犲劚绾惧鏌熼悙顒€澧繛灏栨櫇閳ь剝顫夊ú鏍洪妸鈺傚仼?tableId闂傚倸鍊风欢姘焽瑜嶈灋闁哄啫鐗嗙粻鎺楁煟閻樻鐓秛mnId 闂傚倸鍊烽懗鍫曗€﹂崼銏″床闁规壆澧楅崑顏堟煕閵夛絽濡挎い鈺冨厴閺屾盯顢曢悩鎻掑缂備讲鍋撻柛宀€鍋為悡鏇㈡煙閼割剙濡芥繛鍛缁绘盯宕煎┑鍫濈厽闂?
 * - 闂傚倷娴囬褏鑺遍懖鈺佺筏濠电姵鐔紞鏍ь熆閼搁潧濮堥柛搴★攻閵囧嫰寮崒姘闂?闂傚倷娴囬褏鈧稈鏅濈划娆撳箳濡炲皷鍋撻崘顔煎耿婵炴垼椴搁弲鈺呮倵閸忓浜鹃梺鍛婃处閸撴艾鈻嶉弽褉鏀芥い鏃傜叓椤忓牊鍊堕柛顐犲劚绾惧鏌熼悙顒€澧繛灏栨櫇閳ь剝顫夊ú鏍洪妸鈺傚仼?compareTableId闂傚倸鍊风欢姘焽瑜嶈灋闁哄啫鐗嗙粻鎺楁煟閻樼偣鈧檵areColumnId 闂傚倸鍊烽懗鍫曗€﹂崼銏″床闁规壆澧楅崑顏堟煕閵夛絽濡挎い鈺冨厴閺屾盯顢曢悩鎻掑缂備讲鍋撻柛宀€鍋為悡鏇㈡煙閼割剙濡芥繛鍛缁绘盯宕煎┑鍫濈厽闂?
 * - 闂傚倸鍊峰ù鍥ь浖閵娾晜鍤勯柤绋跨仛濞呯姵淇婇妶鍌氫壕闂佷紮绲介悘姘跺箯閸涱垰瀵查柡鍥╁枑閺嗗﹪姊绘担鍝ョШ闁稿锕ョ粋宥呪枎閹寸偛搴婇梺绯曞墲缁嬫帡鎮″▎鎰╀簻闁圭偓顨呴崯鐗堝閸垻纾藉ù锝囶焾缂嶄線鏌涘Δ浣糕枙鐎殿喖顭峰畷銊╊敇瑜庨崓鐢告倵鐟欏嫭绀€婵炲眰鍔戦幃鎯洪鍛嫼缂備礁顑堝▔鏇犵不娴煎瓨鐓曢柣鏇氱閻忥妇鈧鍠楁繛濠囧箖閵忋倖鍋傞幖杈剧秵閸炶淇婇悙顏勨偓鏍哄澶婄；闁瑰墽绻濈换鍡樸亜閹扳晛鐏柛瀣ㄥ劜椤ㄣ儵鎮欓弶鎴炶癁婵犵绱曢崗妯讳繆閹间礁唯鐟滄粓宕悽鍛娾拻?
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class DqcRuleDefServiceImpl implements IDqcRuleDefService {

    private final DqcRuleDefMapper baseMapper;
    private final DqcPlanRuleMapper planRuleMapper;
    private final DatasourceHelper datasourceHelper;
    private final MetadataTableMapper metadataTableMapper;
    private final MetadataColumnMapper metadataColumnMapper;

    @Override
    public TableDataInfo<DqcRuleDefVo> pageRuleList(DqcRuleDefBo bo, PageQuery pageQuery) {
        log.info("[DQC] 闂傚倷娴囧畷鐢稿窗閹扮増鍋￠柕澹偓閸嬫挸顫濋悡搴♀拫閻庤娲栫紞濠囥€佸☉銏″€烽柤纰卞墻濡叉挳姊绘担铏广€婇柛鎾寸箘缁瑨绠涘☉妯煎姦濡炪倖宸婚崑鎾淬亜閿曞倹娑фい鏇稻缁绘繂顫濋鈹垮妽閵囧嫰顢橀悢椋庝淮濠?- ruleName={}, ruleCode={}, page={}/{}",
                bo.getRuleName(), bo.getRuleCode(),
                pageQuery.getPageNum(), pageQuery.getPageSize());

        // 闂傚倸鍊风粈渚€骞栭锔绘晞闁告侗鍨崑鎾愁潩閻撳骸顫紓浣介哺閹瑰洭鐛Ο鍏煎珰闁艰壈鍩栭幉浼存⒒娓氣偓濞佳囁囨禒瀣亗闁割偁鍎遍崥褰掓煕閺囥劌鐏￠柣鎾寸☉椤法鎹勯悜姗嗘闂佺顑嗛幑鍥х暦閿濆棗绶炴俊顖濇〃缂?
        QueryWrapper<DqcRuleDef> wrapper = baseMapper.compatibleSelect(buildQueryWrapper(bo));
        var page = baseMapper.selectPage(pageQuery.build(), wrapper);
        List<DqcRuleDefVo> voList = MapstructUtils.convert(page.getRecords(), DqcRuleDefVo.class);

        // 濠电姷鏁告繛鈧繛浣冲泚鍥ㄧ鐎ｎ亞锛涢梺鍛婃处閸ㄤ即宕掗妸鈺傜厸鐎广儱楠搁獮鏍磼閳ь剟宕掗悙瀵稿幈闂佺鍩囬崝宥呪枍閸℃绠鹃柛顐ｇ箘閸╋綁鏌″畝鈧崰鎾诲箯閻樿鐏抽柧蹇ｅ亞娴滅偛鈹戦悜鍥╁埌婵炲眰鍊濋弫鍐敂閸垹绁︽繛鎾村焹閸嬫挻銇勯姀锛勬创濠碘€崇埣瀹曞爼鍩℃繝鍐冪喖姊绘担钘夊惞闁稿鍋熺划娆掔疀濞戞顦悗鍏夊亾闁告洦鍋嗛悾娲⒑閸濆嫭鍌ㄩ柛銊ь攰缁绘艾鈹戦悙鑸靛涧缂佽弓绮欓獮妤€顭ㄩ崼婵堫啈闂佺粯鏌ㄩ幗婊呯不妤ｅ啯鐓曟い鎰Т閸旀粍绻涢幊宄版处閻撴瑦銇勯弮鍥撶紒鈧€ｎ亖鏀介柍銉ョ－閹界娀鏌熷畡鐗堝櫧闁瑰弶鎸冲畷鐔碱敆婢跺﹥娅冮梻鍌氬€烽懗鍫曘€佹繝鍥舵晪闁哄稁鍘肩粣妤佷繆閵堝懎鏆㈠┑顔挎珪閵囧嫰寮崒娑欑彧闂佸磭鎳撶粔褰掑蓟瀹ュ鐓涘ù锝呮啞濞堛劎绱撴担浠嬪摵闁绘濞€瀵?
        enrichMetadataInfo(voList);

        log.info("[DQC] 闂傚倷娴囧畷鐢稿窗閹扮増鍋￠柕澹偓閸嬫挸顫濋悡搴♀拫閻庤娲栫紞濠囥€佸☉銏″€烽柤鑹板煐閹蹭即姊绘笟鈧褔藝娴犲鍋傞柛顐犲劚閸氬綊鏌涢弴銊ュ箻缁炬崘妫勯湁闁挎繂鐗滃鎰版倶韫囧骸宓嗛柡?- total={}, records={}", page.getTotal(), voList.size());
        return new TableDataInfo<>(voList, page.getTotal());
    }

    @Override
    public DqcRuleDefVo getRuleById(Long id) {
        DqcRuleDefVo vo = MapstructUtils.convert(requireAccessibleRule(id), DqcRuleDefVo.class);
        if (vo != null) {
            enrichMetadataInfo(List.of(vo));
        }
        return vo;
    }

    @Override
    public Long insertRule(DqcRuleDefBo bo) {
        // 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顓濇勃闁诡垎鍕偓顓㈡⒒娴ｅ憡鍟炴繛璇х畵瀹曟垿宕熼娑樷偓鍫曟煟閺傚灝鎮戦柛濠勬暬閹嘲鈻庤箛鎿冧紑闂佺粯鎼╅崹宕囨閹烘鍋愰梻鍫熺⊕閻忓牓姊烘潪鎵槮缂佸鎳撻悾鐑芥偂鎼存ɑ鏂€闂佺硶鍓濋〃鍫ユ偡閺屻儲鈷掑ù锝堝Г閵嗗啴鏌ょ€圭姴鐓愰柡鍛版硾铻栭柍褜鍓熼幃楣冨垂椤愶絽顎撳Δ鐘靛仜閵囨鍒掗幘顔解拺闁告稑顭▓姗€鏌涚€ｎ偆顬兼い銈呭濮婄粯鎷呴崷顓熻弴闂佸憡鏌ㄩ惉濂稿焵椤掍胶鐓柛妤佸▕楠炲啴寮舵惔鎾寸€婚梺鐟邦嚟閸嬫盯鎮甸悧鍫㈢闁瑰墽顥愭竟姗€鏌ㄩ弴顏勵洭缂?
        validateRuleDefinition(bo);

        DqcRuleDef entity = MapstructUtils.convert(bo, DqcRuleDef.class);
        setDefaultValues(entity);
        baseMapper.insert(entity);

        log.info("[DQC] 闂傚倸鍊风粈渚€骞栭锕€纾归柣鐔煎亰閻斿棙淇婇婵嗕汗缁惧彞绮欓弻鐔煎箲閹邦剛鍘梺鍝ュТ濡繈寮诲☉銏犲嵆闁靛鍎扮花濠氭⒑?- id={}, ruleName={}", entity.getId(), entity.getRuleName());
        return entity.getId();
    }

    @Override
    public int updateRule(DqcRuleDefBo bo) {
        requireAccessibleRule(bo.getId());
        validateRuleDefinition(bo);

        DqcRuleDef entity = MapstructUtils.convert(bo, DqcRuleDef.class);
        int result = baseMapper.updateById(entity);

        log.info("[DQC] 闂傚倸鍊风粈渚€骞栭鈷氭椽濡舵径瀣槐闂侀潧艌閺呮盯鎷戦悢灏佹斀闁绘ɑ褰冮顏堟煕閻樺弶顥㈤柡灞剧洴瀵挳濡搁妷銈囩泿闂?- id={}, result={}", bo.getId(), result);
        return result;
    }

    @Override
    public int updateEnabled(Long id, String enabled) {
        requireAccessibleRule(id);
        DqcRuleDef patch = new DqcRuleDef();
        patch.setId(id);
        patch.setEnabled(enabled);
        return baseMapper.updateById(patch);
    }

    @Override
    public int deleteRule(Long[] ids) {
        for (Long id : ids) {
            requireAccessibleRule(id);
        }
        return baseMapper.deleteBatchIds(List.of(ids));
    }

    @Override
    public List<DqcRuleDefVo> listByPlanId(Long planId) {
        // 闂傚倸鍊风粈渚€骞栭銈嗗仏妞ゆ劧绠戠壕鍧楁煙缂併垹娅橀柡浣割儐娣囧﹪濡堕崨顔兼濠碘槅鍋呴敃銏ゅ箖瀹勬壋鏋庨煫鍥ㄦ惄娴煎矂姊虹悰鈥充壕?闂傚倷娴囧畷鐢稿窗閹扮増鍋￠柕澹偓閸嬫挸顫濋悡搴♀拫閻庤娲栫紞濠囥€佸☉銏″€烽柤纰卞墻濡差垶姊虹拠鏌ヮ€楁繝鈧潏銊﹀弿鐎规洖娲ら崹?
        List<DqcPlanRule> planRules = planRuleMapper.selectCompatibleByPlanId(planId);

        if (planRules.isEmpty()) {
            return List.of();
        }

        List<Long> ruleIds = planRules.stream()
            .map(DqcPlanRule::getRuleId)
            .toList();

        List<DqcRuleDefVo> voList = MapstructUtils.convert(baseMapper.selectCompatibleBatchIds(ruleIds), DqcRuleDefVo.class);

        // 濠电姷鏁告繛鈧繛浣冲泚鍥ㄧ鐎ｎ亞锛涢梺鍛婃处閸ㄤ即宕掗妸鈺傜厸鐎广儱楠搁獮鏍磼閳ь剟宕掗悙瀵稿幈闂佺鍩囬崝宥呪枍閸℃绠鹃柛顐ｇ箘閸╋綁鏌″畝鈧崰鎾诲箯閻樿鐏抽柧蹇ｅ亞娴滅偛鈹戦悜鍥╁埌婵炲眰鍊濋弫鍐敂閸垹绁?
        enrichMetadataInfo(voList);
        return voList;
    }

    @Override
    @DS("bigdata")
    @DSTransactional
    public int bindRules(Long planId, List<Long> ruleIds) {
        for (Long ruleId : ruleIds) {
            requireAccessibleRule(ruleId);
        }
        // 闂傚倸鍊烽懗鍫曗€﹂崼銏″床闁规壆澧楅崑瀣煕閳╁啰鈽夐柛銊ュ€婚幉鎼佹偋閸繄鐟查梺鎶芥敱濡啴寮婚弴銏犻唶婵犻潧娴傚Λ锟犳⒑閹稿海鈽夋い鎴濐樀瀵鈽夊鍛澑闂佽宕橀崺鏍倵椤旂晫绠?
        planRuleMapper.delete(
            Wrappers.<DqcPlanRule>lambdaQuery()
                .eq(DqcPlanRule::getPlanId, planId)
        );

        // 闂傚倸鍊风粈渚€骞栭锕€纾归柣鐔煎亰閻斿棙鎱ㄥ璇蹭壕閻犱警鍨堕弻娑㈠箛闂堟稒鐏堝┑鈽嗗亝閿曘垽寮婚埄鍐ㄧ窞濠电姴瀚搹搴ㄦ偡濠婂嫬顥嬪┑鐐╁亾闂?
        int sortOrder = 1;
        for (Long ruleId : ruleIds) {
            DqcPlanRule planRule = new DqcPlanRule();
            planRule.setPlanId(planId);
            planRule.setRuleId(ruleId);
            planRule.setSortOrder(sortOrder++);
            planRuleMapper.insert(planRule);
        }

        return ruleIds.size();
    }

    @Override
    public List<DqcRuleDefVo> listRule(DqcRuleDefBo bo) {
        QueryWrapper<DqcRuleDef> wrapper = baseMapper.compatibleSelect(buildQueryWrapper(bo));
        List<DqcRuleDefVo> voList = MapstructUtils.convert(baseMapper.selectList(wrapper), DqcRuleDefVo.class);
        enrichMetadataInfo(voList);
        return voList;
    }

    // ==================== RuoYi 闂傚倸鍊风粈渚€骞栭銈囩煋闁哄鍤氬ú顏勎у璺猴躬濡嘲顪冮妶鍡欏⒈闁稿绋戣灋婵せ鍋撻柟顔煎槻閳诲氦绠涢幙鍐х磻闂備線娼уΛ妤呮晝椤忓牆绠栨俊銈呮噹閹硅埖銇勯幘璺轰粧闁规潙鍢茶灃?====================

    @Override
    public TableDataInfo<DqcRuleDefVo> queryPageList(DqcRuleDefBo bo, PageQuery pageQuery) {
        return pageRuleList(bo, pageQuery);
    }

    @Override
    public DqcRuleDefVo queryById(Long id) {
        return getRuleById(id);
    }

    @Override
    public Long insertByBo(DqcRuleDefBo bo) {
        return insertRule(bo);
    }

    @Override
    public int updateByBo(DqcRuleDefBo bo) {
        return updateRule(bo);
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return deleteRule(ids.toArray(new Long[0]));
    }

    // ==================== 缂傚倸鍊搁崐椋庣矆娓氣偓钘濇い鏇楀亾闁诡喚鍋ら弫鍐焵椤掑嫭鏅濋柕蹇ョ磿閻熷綊鏌嶈閸撴瑩顢氶敐澶樻晢闁稿本绋戦弸鍌炴⒑閸涘﹥澶勯柛妯虹秺閸┾偓?====================

    /**
     * 闂傚倸鍊风粈渚€骞栭锔绘晞闁告侗鍨崑鎾愁潩閻撳骸顫紓浣介哺閹瑰洭鐛Ο鍏煎珰闁艰壈鍩栭幉浼存⒒娓氣偓濞佳囁囨禒瀣亗闁割偁鍎遍崥褰掓煕閺囥劌鐏￠柣鎾寸☉椤法鎹勯悜姗嗘闂佺顑嗛幑鍥х暦閿濆棗绶炴俊顖濇〃缂?
     */
    private QueryWrapper<DqcRuleDef> buildQueryWrapper(DqcRuleDefBo bo) {
        QueryWrapper<DqcRuleDef> queryWrapper = new QueryWrapper<>();
        LambdaQueryWrapper<DqcRuleDef> wrapper = queryWrapper.lambda();

        // 闂傚倸鍊烽懗鍫曗€﹂崼銏″床闁圭増婢橀悿顔姐亜閺嶎偄浠滄慨瑙勭叀閺岋綁寮崒姘粯缂備胶濮村鍫曞Φ閸曨垰鍗虫俊銈傚亾濞存粓绠栧鍝勭暦閸ャ劌娈岀紓浣割槺閹虫捇鎮惧畡鎵虫斀閻庯綆鈧厸鏅濋幉鍛婃償閵婏箑浠奸梻渚囧墮缁夌敻鎮″☉銏″€堕柣鎰邦杺閸ゆ瑩鏌涢弬璇插姕闁靛洤瀚板鎾偆娴ｅ湱鏉归梻浣芥〃缁€浣虹矓閹绢噯缍栨繝闈涱儛閺佸啴鏌嶉崹娑欐珕闁诲骸銈稿?tableId 濠电姷鏁搁崑娑㈩敋椤撶喎鍨旈悗鐢殿焾缁躲倝鏌熼锝囦汗?targetDsId
        if (ObjectUtil.isNotNull(bo.getTableId())) {
            wrapper.eq(DqcRuleDef::getTableId, bo.getTableId());
        }

        wrapper.like(StringUtils.isNotBlank(bo.getRuleName()), DqcRuleDef::getRuleName, bo.getRuleName())
            .like(StringUtils.isNotBlank(bo.getRuleCode()), DqcRuleDef::getRuleCode, bo.getRuleCode())
            .eq(ObjectUtil.isNotNull(bo.getTemplateId()), DqcRuleDef::getTemplateId, bo.getTemplateId())
            .eq(StringUtils.isNotBlank(bo.getRuleType()), DqcRuleDef::getRuleType, bo.getRuleType())
            .eq(StringUtils.isNotBlank(bo.getDimensions()), DqcRuleDef::getDimensions, bo.getDimensions())
            .eq(StringUtils.isNotBlank(bo.getEnabled()), DqcRuleDef::getEnabled, bo.getEnabled())
            .orderByDesc(DqcRuleDef::getCreateTime);
        return queryWrapper;
    }

    /**
     * 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顓濇勃闁诡垎鍕偓顓㈡⒒娴ｅ憡鍟炴繛璇х畵瀹曟垿宕熼娑樷偓鍫曟煟閺傚灝鎮戦柛濠勬暬閹嘲鈻庤箛鎿冧紑闂佺粯鎼╅崹宕囨?
     * <p>
     * 闂傚倸鍊烽懗鍫曗€﹂崼銏″床闁圭増婢橀悿顔姐亜閺嶎偄浠滄慨瑙勭叀閺岋綁寮崒姘粯缂備胶濮村鍫曞Φ閸曨垰鍗虫俊銈傚亾濞存粓绠栧鍝勭暦閸ャ劌娈岀紓浣割槺閹虫捇鎮惧畡鎵虫斀閻庯綆鈧厸鏅濋幉鍛婃償閵婏箑浠奸梺鍛婎殘閸婏綁寮崼鐔蜂汗闂佽偐鈷堥崜娑㈠礉椤曗偓濮婄儤瀵煎▎鎴犳殸濡炪倧濡囬弫璇差嚕婵犳艾宸濋柡澶嬪灥瀵兘妫呴銏″婵炲弶鐗滅划鈺傜附閸涘ň鎷?
     * 1. 闂傚倸鍊搁…顒勫磻閸曨個娲Χ婢跺﹦鐤囧┑顔姐仜閸嬫挸鈹戦敍鍕垫缂佺姵绋戦埥澶娢熺喊鍗炴暪闂傚倷鐒﹂幃鍫曞磿濠婂牆宸濇い鏃傛櫕濡?tableId
     * 2. 闂傚倷娴囬褏鈧稈鏅濈划娆撳箳濡炲皷鍋撻崘顔煎耿婵炴垼椴搁弲鈺呮倵閸忓浜鹃梺閫炲苯澧撮柟顖楀亾濡炪倕绻愬ù鍌毼熸担鍓叉富闁靛牆楠告禍婊勩亜閿曞倹娑ч柣锝呭槻椤繈顢栭懞銉р偓濠氭椤愩垺澶勬繛鍙夌矒閹偓娼忛妸銈囩畾闂佺粯鍔︽禍婊堝焵椤掍緡娈橀柛鎺撳浮椤㈡稑鈹冮幆褌澹曢梺鍓茬厛閸嬪懘鎯冮幋锔界厓?columnId
     * 3. 闂傚倷娴囧畷鍨叏閸偆顩插ù鐘差儏缁€澶屸偓鍏夊亾闁告洦鍓欐禒閬嶆偡濠婂啰效闁?闂傚倷娴囧畷鍨叏閸偆顩插ù鐘差儏缁€澶愮叓閸ャ劎鈽夋潻婵嬫⒑閸涘﹤濮﹀ù婊勭箞瀹曚即宕卞☉娆戝幈闂佸搫娲㈤崝灞剧濠婂牊鐓熼煫鍥ㄦ礀閺嬫盯鏌ｉ幙鍐ㄤ喊鐎规洘鍎奸ˇ鎶芥煕閵堝懏鍠橀柡灞炬礋瀹曞崬螖閸愵亶鍟嬫俊銈囧Х閸嬬偤宕濆▎蹇曟殾闁诡垶鍋婂Σ楣冩⒑閸濆嫭顥欓柛妤佸▕楠炲啫螖閸涱喖浠洪梺鍓茬厛閸嬪嫬鈻撳畝鈧槐鎾存媴闂堟稑顬夐梺缁樻惈缁绘繈鐛?compare ID
     * 4. 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顖氱疀妞ゆ帒鍋嗗Σ顖炴⒒娴ｅ憡鍟為柛鏂跨箻瀵彃顭ㄩ崘锝嗙亖闂佸壊鍋呭ú姗€鎮″▎鎾村仯闁搞儻绲洪崑鎾绘惞椤愩倓绮氬┑锛勫亼閸婃牠寮婚妸銉冩椽鍩￠崨顓囷箓鏌熼悧鍫熺凡闁告濞婇弻锝夊籍閳ь剟宕濋崨瀛樻櫇闁稿本绋撻崢鍗炩攽椤旂煫顏呮櫠娴犲鍋╅梺顒€绉甸悡鐔煎箳閹惰棄绀夐柟瀛樼箘閺?
     */
    private void validateRuleDefinition(DqcRuleDefBo bo) {
        // 1. 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顖氱疀妞ゆ挾鍋涙竟鍡樹繆閻愵亜鈧牕顫忔繝姘柧妞ゆ劧绠戦崹鍌氣攽閸屾碍鍟為柛瀣У缁绘盯骞嬮悙闈涒吂婵犲痉銈呬汗缂佽鲸甯￠幃婊堝煛娴ｅ嘲顥氬┑鐘殿暜缁辨洟宕戦幋锕€纾规俊銈呭暙閸ㄦ繃銇勯弽銊ヮ棜闁稿鎹囧畷褰掝敃閿濆洦顓婚梻?
        if (bo.getTableId() == null) {
            throw new ServiceException("Please select a target table.");
        }

        // 2. 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顖氱疀妞ゆ挾鍋涙竟鍡樹繆閻愵亜鈧牕顫忔繝姘柧妞ゆ劧绠戦崹鍌氣攽閸屾碍鍟為柛瀣У缁绘盯骞嬮悙闈涒吂婵犵鈧偨鍋㈤柡灞剧☉铻ｇ紓浣姑壕鎶芥倵濞堝灝娅橀柛瀣躬楠炲啳顦圭€规洜鍘ч埞鎴﹀醇濞戞ü鍠婇梻鍌欐祰椤曆呪偓娑掓櫇缁瑩骞掑Δ浣规珨濠电姷鏁告慨顓㈠磻?
        MetadataTable table = metadataTableMapper.selectById(bo.getTableId());
        if (table == null) {
            throw new ServiceException("闂傚倸鍊烽懗鍫曞磿閻㈢鐤鹃柍鍝勬噹缁愭淇婇妶鍛櫤闁稿顦甸弻銊モ攽閸℃ê顦╅梺绋款儐閿曘垽寮婚弴鐔虹闁割煈鍠栨慨搴ㄦ煟閵忊晛鐏℃い銊ワ躬瀵鍩勯崘鈺侇€撻梺鍛婄箓鐎氱兘寮抽銏″€甸悷娆忓鐏忣偊鏌ｉ幒鐐差洭缂侇喗鐟﹀鍕節鎼粹剝鍊梻浣告啞娓氭宕归柆宥呯疅濠靛倸鎲￠埛? " + bo.getTableId());
        }

        // 3. 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顖氱疀妞ゆ挾鍋涙竟鍡樹繆閻愵亜鈧牕顫忔繝姘柧妞ゆ劧绠戦崹鍌氣攽閸屾碍鍟為柣鎾寸☉闇夐柨婵嗘噺閹牊銇勯敐鍫濅汗闁逞屽墲椤骞愭搴ｇ焼濞撴埃鍋撻柛鈺冨仱瀹曟﹢顢欓懖鈺冩澑婵＄偑鍊栧濠氬磻閹剧粯鍋ｅù锝呮贡閸欌偓闂佽鍠涢～澶愬箯閸涙潙宸濆┑鐘插€峰ǎ顔界節?
        datasourceHelper.getSysDatasource(table.getDsId());

        // 4. 闂傚倷娴囬褏鈧稈鏅濈划娆撳箳濡炲皷鍋撻崘顔煎耿婵炴垼椴搁弲鈺呮倵閸忓浜鹃梺閫炲苯澧撮柟顖楀亾濡炪倕绻愬ù鍌毼熸担鍓叉富闁靛牆楠告禍婊勩亜閿曞倹娑ч柣锝呭槻椤繈顢栭懞銉р偓濠氭椤愩垺澶勬繛鍙夌矒閹偓娼忛妸銈囩畾闂佺粯鍔︽禍婊堝焵椤掍緡娈橀柛鎺撳浮椤㈡稑鈹冮幆褌澹曢梺鍓茬厛閸嬪懘鎯冮幋锔界厓?columnId
        if ("COLUMN".equals(bo.getApplyLevel()) && bo.getColumnId() == null) {
            throw new ServiceException("Column-level rules must select a target column.");
        }

        // 5. 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顖氱疀妞ゆ挾鍋涙竟鍡樹繆閻愵亜鈧牕顫忔繝姘柧妞ゆ劧绠戦崹鍌氣攽閸屾碍鍟為柛濠傜仛缁绘盯骞嬮悙鍨櫧濠电偛鐗婂ú姗€濡甸崟顖氭閻犳亽鍔庨崝顖炴倵鐟欏嫭绀冩い銊ワ工閻ｅ嘲顫滈埀顒勫春閿熺姴纾兼繛鎴濆船閺呮瑩姊婚崒娆掑厡妞ゃ垹锕、姘跺冀椤撶偟鐛ラ梺鍝勭▉閸樹粙宕戦妶澶嬬厪濠电偟鍋撳▍鍛存煙绾懎鐓愰柕鍥у椤㈡﹢鎮㈡搴剬缂傚倷璁查崑鎾绘煕閺囥劌浜愰柡鈧禒瀣厓闁靛鍎遍弳閬嶆煃瑜滈崜娆戠礊婵犲洤绠栨俊顖濐嚙缁剁偤鏌熼柇锕€澧版い鏂挎喘濮婃椽骞栭悙鎻掑Η闂佸憡渚楅崰妤呮儊閻愭祴鏀?columnId闂?
        if (bo.getColumnId() != null) {
            MetadataColumn column = metadataColumnMapper.selectById(bo.getColumnId());
            if (column == null) {
                throw new ServiceException("闂傚倸鍊烽懗鍫曞磿閻㈢鐤鹃柍鍝勬噹缁愭淇婇妶鍛櫤闁稿顦甸弻銊モ攽閸℃顦遍梺绋款儐閹告悂鍩ユ径濞炬瀻婵☆垵宕甸弳锔戒繆閻愵亜鈧牠寮婚妸鈺傚剹闁稿本姘ㄩ弳锔戒繆閵堝懏鍣圭紒鈧崘顔界厓闁告繂瀚埀顒€缍婇、娆忣吋婢跺鎷虹紒缁㈠幖閹峰宕甸崘顔界厱闁绘娅曠粈鍫㈢磼椤曞懎骞楃€垫澘瀚禒锔剧矙婢剁顥氭繝鐢靛仦閸ㄥ爼顢旈鍫晜闁告洘鍎奸埀? " + bo.getColumnId());
            }
            // 闂傚倸鍊风粈渚€骞栭銈囩煓闁告洦鍘藉畷鍙夌節闂堟侗鍎愰柛瀣戠换娑㈠幢濡櫣浜堕梺绋款儐閹告悂鍩ユ径濞炬瀻婵☆垵宕甸弳锔戒繆閻愵亜鈧牠寮婚妸鈺傚剹闁稿本鍑归崵鏇灻归崗鍏煎剹闁轰礁绉甸幈銊ヮ潨閸℃绠归梺鍛婃尪閸旀垿寮婚敐鍫㈢杸闁哄洨鍋橀幋鐑芥⒑缁嬫鍎愭い顓炴喘閹崇偤鏌嗗搴㈡櫌闂佺鏈粙鎴︽偟娴煎瓨鈷戦梻鍫熶緱濡插爼鏌涢弬鍖¤含闁糕斁鍋?
            if (!column.getTableId().equals(bo.getTableId())) {
                throw new ServiceException("The selected column does not belong to the target table.");
            }
        }

        // 6. 闂傚倷娴囧畷鍨叏閸偆顩插ù鐘差儏缁€澶愮叓閸ャ劎鈽夋潻婵嬫⒑閸涘﹤濮﹀ù婊勭箞瀹曚即宕卞☉娆戝幈闂佸搫娲㈤崝灞剧濠婂牊鐓熼煫鍥ㄦ礀閺嬫盯鏌ｉ幙鍐ㄤ喊鐎规洘鍎奸ˇ鎶芥煕閵堝懏鍠橀柡灞炬礋瀹曞崬螖閸愵亶鍟嬫俊銈囧Х閸嬬偤宕濆▎蹇曟殾闁诡垶鍋婂Σ楣冩⒑?compareTableId
        if ("CROSS_TABLE".equals(bo.getApplyLevel()) && bo.getCompareTableId() == null) {
            throw new ServiceException("Cross-table rules must select a compare table.");
        }

        // 7. 闂傚倷娴囧畷鍨叏閸偆顩插ù鐘差儏缁€澶屸偓鍏夊亾闁告洦鍓欐禒閬嶆偡濠婂啰效闁糕斂鍨介獮妯尖偓娑櫭鎾绘⒑閸涘﹤濮夐柛瀣尵缁辩偤宕奸妷锔规嫼闂佸憡绋戦敃銈囩箔濮樿埖鐓熼柣鏃€娼欓崝銈夋煙楠炲灝鐏查柛鈹惧亾濡炪倖宸婚崑鎾淬亜椤忓嫬鏆ｅ┑鈥崇埣瀹曞爼鍩℃担渚仹闂備浇顕ф鎼佹倶濮橆剦娼╅柕濞炬櫅閻?compareColumnId
        if ("CROSS_FIELD".equals(bo.getApplyLevel())) {
            if (bo.getCompareTableId() == null) {
                throw new ServiceException("Cross-field rules must select a compare table.");
            }
            if (bo.getCompareColumnId() == null) {
                throw new ServiceException("Cross-field rules must select a compare column.");
            }
        }

        // 8. 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顓濇勃缂佸銇樻竟鏇熺箾鏉堝墽鍒伴柛鐘宠壘椤洭骞囬悧鍫㈠幗闂佹寧妫侀褔寮搁妶澶嬬厵妞ゆ棁顕у畵鍡椻攽閳╁啯鍊愬┑锛勫厴婵＄兘濡疯閹虫牠姊婚崒娆愮グ婵℃ぜ鍔戦幊妤呮嚋閸偅鐝峰┑掳鍊愰崑鎾绘煟閿濆懐浠涢柟宄版噽閹即鍨鹃崗鍛棜婵犵數鍋為崹鍫曨敂椤忓牜鏁傞柛鏇熷劶閳?
        if (bo.getCompareTableId() != null) {
            MetadataTable compareTable = metadataTableMapper.selectById(bo.getCompareTableId());
            if (compareTable == null) {
                throw new ServiceException("闂傚倷娴囬褏鑺遍懖鈺佺筏濠电姵鐔紞鏍ь熆閼搁潧濮堥柛搴★攻閵囧嫰寮崒姘闂佺顑嗛敃銏ゅ蓟閺囩喓绠鹃柛顭戝枛婵酣鏌ｉ姀鈺佺仭妞ゃ劌锕濠氬焺閸愨晛顎撻梺鍛婄箓鐎氱兘寮抽銏″€甸悷娆忓鐏忣偊鏌ｉ幒鐐差洭缂侇喗鐟﹀鍕節鎼粹剝鍊梻浣告啞娓氭宕归柆宥呯疅濠靛倸鎲￠埛? " + bo.getCompareTableId());
            }

            // 9. 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顓濇勃缂佸銇樻竟鏇熺箾鏉堝墽鍒伴柛鐘宠壘椤洭骞囬悧鍫㈠幗闂佹寧妫侀褔寮搁妶澶嬬厓鐟滄粓宕滃杈╃煓闁圭儤銇滈埀顒€鍟村畷鍗炩槈鏉堛劍娅撻柣搴″帨閸嬫捇鏌嶈閸撶喖宕洪埀顒併亜閹哄秷鍏岄柍顖涙礋閺屻劌顫濋鐘电槇闂?
            if (bo.getCompareColumnId() != null) {
                MetadataColumn compareColumn = metadataColumnMapper.selectById(bo.getCompareColumnId());
                if (compareColumn == null) {
                    throw new ServiceException("闂傚倷娴囬褏鑺遍懖鈺佺筏濠电姵鐔紞鏍ь熆閼搁潧濮堥柛搴★攻閵囧嫰寮崼鐔告闂佺顑嗛幐鎼佸煡婢跺ň鏋庢俊顖濆吹閺嗭附淇婇悙顏勨偓鏍蓟閵娾晜鍎楅柛灞炬皑閺嗭附淇婇妶鍛櫣缂佲偓閸愵喗鐓冮柛婵嗗閳ь剙缍婇、娆忣吋婢跺鎷虹紒缁㈠幖閹峰宕甸崘顔界厱闁绘娅曠粈鍫㈢磼椤曞懎骞楃€垫澘瀚禒锔剧矙婢剁顥氭繝鐢靛仦閸ㄥ爼顢旈鍫晜闁告洘鍎奸埀? " + bo.getCompareColumnId());
                }
                if (!compareColumn.getTableId().equals(bo.getCompareTableId())) {
                    throw new ServiceException("The selected compare column does not belong to the compare table.");
                }
            }
        }

        // 10. 闂傚倸鍊烽懗鍫曞储瑜旈妴鍐╂償閵忋埄娲稿┑鐘诧工閻楀﹪宕戦埡鍛厽闁逛即娼ф晶浼存煃缂佹ɑ绀€闁宠鍨跺鍕偓锝庡亝椤ｇ硳闂傚倷娴囧畷鐢稿窗閹扮増鍋￠柕澹偓閸嬫挸顫濋悡搴♀拫閻庤娲栫紞濠囥€佸☉妯锋閺夊牜鐓堥崯宥夋煟鎼粹€冲辅闁稿鎹囬弻娑㈠箛閸忓摜鍑瑰?
        if (CustomSqlExecutor.TYPE.equalsIgnoreCase(bo.getRuleType())) {
            CustomSqlSecuritySupport.validateRuleExpr(bo.getRuleExpr());
        }
    }

    /**
     * 濠电姷鏁告繛鈧繛浣冲泚鍥ㄧ鐎ｎ亞锛涢梺鍛婃处閸ㄤ即宕掗妸鈺傜厸鐎广儱楠搁獮鏍磼閳ь剟宕掗悙瀵稿幈闂佺鍩囬崝宥呪枍閸℃绠鹃柛顐ｇ箘閸╋綁鏌″畝鈧崰鎾诲箯閻樿鐏抽柧蹇ｅ亞娴滅偛鈹戦悜鍥╁埌婵炲眰鍊濋弫鍐敂閸垹绁︽繛鎾村焹閸嬫挻銇勯姀锛勬创濠碘€崇埣瀹曞爼鍩℃繝鍐冪喖姊绘担钘夊惞闁稿鍋熺划娆掔疀濞戞顦悗鍏夊亾闁告洦鍋嗛悾娲⒑閸濆嫭鍌ㄩ柛銊ь攰缁绘艾鈹戦悙鑸靛涧缂佽弓绮欓獮妤€顭ㄩ崼婵堫啈闂佺粯鏌ㄩ幗婊呯不妤ｅ啯鐓曟い鎰Т閸旀粍绻涢幊宄版处閻撴瑦銇勯弮鍥撶紒鈧€ｎ亖鏀介柍銉ョ－閹界娀鏌熷畡鐗堝櫧闁瑰弶鎸冲畷鐔碱敆婢跺﹥娅冮梻鍌氬€烽懗鍫曘€佹繝鍥舵晪闁哄稁鍘肩粣妤佷繆閵堝懎鏆㈠┑顔挎珪閵囧嫰寮崒娑欑彧闂佸磭鎳撶粔褰掑蓟瀹ュ鐓涘ù锝呮啞濞堛劎绱撴担浠嬪摵闁绘濞€瀵?
     * <p>
     * 闂傚倸鍊搁崐椋庢閿熺姴纾婚柛娑卞枤閳瑰秹鏌ц箛姘兼綈鐎?tableId闂傚倸鍊风欢姘焽瑜嶈灋闁哄啫鐗嗙粻鎺楁煟閻樻鐓秛mnId闂傚倸鍊风欢姘焽瑜嶈灋闁哄啫鐗嗙粻鎺楁煟閻樼偣鈧檵areTableId闂傚倸鍊风欢姘焽瑜嶈灋闁哄啫鐗嗙粻鎺楁煟閻樼偣鈧檵areColumnId 闂傚倸鍊风粈渚€骞栭銈嗗仏妞ゆ劧绠戠壕鍧楁煙缂併垹娅橀柡浣割儐娣囧﹪濡堕崨顔兼缂備讲鍋撻柛宀€鍋為悡鏇㈡煙閼割剙濡芥繛鍛缁绘盯宕煎┑鍫濈厽闂?
     * 濠电姷鏁告繛鈧繛浣冲泚鍥ㄧ鐎ｎ亞锛涢梺鍛婃处閸ㄤ即宕掗妸鈺傜厸鐎广儱楠告禍妤佺箾瀹€濠侀偗闁哄矉绻濆畷鍫曞煛娴ｅ洨鍋涢湁闁绘ê纾ú鎾煛瀹€鈧崰鎾诲窗婵犲伣鐔告姜閺夋妫滈梻鍌氬€搁崐鎼佸磹缁嬫５娲偐鐠囪尙锛涢梺鐟板⒔缁垶鎮￠悢鍏肩厪濠电偛鐏濋崝姘亜韫囧﹥娅婇柡宀€鍠栭、娆撴嚍閵壯屾Ф缂傚倷绶￠崰妤€螞閸愵煈鍤曢柛鎾茬劍瀹曞鏌曟繝蹇涙闁哥姵娲樼换?
     */
    private void enrichMetadataInfo(List<DqcRuleDefVo> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }

        // 闂傚倸鍊峰ù鍥Υ閳ь剟鏌涚€ｎ偅宕岄柡灞剧洴椤㈡洟鏁愰崱娆樻О闂備浇澹堢亸娆愮箾閳ь剟鏌″畝鈧崰鎾诲窗婵犲伣鐔告姜閺夋妫滈梻鍌氬€风粈渚€骞栭锔藉亱闁糕剝铔嬮崶顒侇棃婵炴垶鐗戦崑鎾诲箳濡も偓缁€鍫澝归敐鍥ㄥ殌闁哄棭鍋呯换娑欐綇閸撗勫仹闂佺绨洪崐婵嬪箖妤ｅ啯鐒肩€广儱妫涢崢閬嶆椤愩垺澶勯柟鍛婃倐瀵娊鎮欓悜妯煎幐闂佺硶鍓濋悷銉╁几濞戞瑯娈?ID
        List<Long> tableIds = voList.stream()
            .map(DqcRuleDefVo::getTableId)
            .filter(ObjectUtil::isNotNull)
            .distinct()
            .toList();

        List<Long> compareTableIds = voList.stream()
            .map(DqcRuleDefVo::getCompareTableId)
            .filter(ObjectUtil::isNotNull)
            .distinct()
            .toList();

        // 闂傚倸鍊风粈浣虹礊婵犲偆鐒界憸蹇曟閻愬绡€闁搞儜鍥紬婵犵數鍋涘Ο濠冪濠婂牊瀚呴柣鏂垮悑閻撳繐顭块懜娈跨劷闁告ɑ鐩弻娑㈠Χ閸涱収浼冮梺鍝勬湰缁嬫牜绮诲☉銏犵闁惧浚鍋呴澶愭⒒娴ｅ憡鎯堟俊顐ｇ洴瀹曚即寮介鐘茬ウ闂佺鎻€靛矂寮繝鍥ㄥ仭濞达綁娼ч埀顒佸姍瀹?
        List<Long> allTableIds = new java.util.ArrayList<>(tableIds);
        allTableIds.addAll(compareTableIds);
        Map<Long, MetadataTable> tableMap = allTableIds.isEmpty()
            ? Map.of()
            : metadataTableMapper.selectBatchIds(allTableIds)
            .stream()
            .collect(java.util.stream.Collectors.toMap(MetadataTable::getId, t -> t, (a, b) -> a));

        // 闂傚倸鍊峰ù鍥Υ閳ь剟鏌涚€ｎ偅宕岄柡灞剧洴椤㈡洟鏁愰崱娆樻О闂備浇澹堢亸娆愮箾閳ь剟鏌″畝鈧崰鎾诲窗婵犲伣鐔告姜閺夋妫滈梻鍌氬€风粈渚€骞栭锔藉亱闁糕剝铔嬮崶顒夋晬闁绘劖娼欐禒閬嶆偡濠婂啰效闁?ID
        List<Long> columnIds = voList.stream()
            .map(DqcRuleDefVo::getColumnId)
            .filter(ObjectUtil::isNotNull)
            .distinct()
            .toList();

        List<Long> compareColumnIds = voList.stream()
            .map(DqcRuleDefVo::getCompareColumnId)
            .filter(ObjectUtil::isNotNull)
            .distinct()
            .toList();

        List<Long> allColumnIds = new java.util.ArrayList<>(columnIds);
        allColumnIds.addAll(compareColumnIds);

        Map<Long, MetadataColumn> columnMap = allColumnIds.isEmpty()
            ? Map.of()
            : metadataColumnMapper.selectBatchIds(allColumnIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(MetadataColumn::getId, c -> c, (a, b) -> a));

        // 濠电姷鏁告繛鈧繛浣冲泚鍥ㄧ鐎ｎ亞锛涢梺鍛婃处閸ㄤ即宕掗妸鈺傜厸鐎广儱鍟俊鍧楁煃閻熸壆效闁哄苯绉归崺鈩冩媴閸涘﹥顓跨紓?
        for (DqcRuleDefVo vo : voList) {
            // 闂傚倸鍊烽懗鍫曞磿閻㈢鐤鹃柍鍝勬噹缁愭淇婇妶鍛櫤闁稿顦甸弻銊モ攽閸℃ê顦╅梺绋款儐閿曘垽寮婚弴鐔虹闁割煈鍠掗崑鎾诲箻閼搁潧搴婇梺绯曞墲缁嬫帡鎮?
            if (vo.getTableId() != null) {
                MetadataTable table = tableMap.get(vo.getTableId());
                if (table != null) {
                    vo.setTableName(table.getTableName());
                    vo.setDsId(table.getDsId());
                    vo.setDsName(table.getDsName());
                }
            }

            // 闂傚倸鍊烽懗鍫曞磿閻㈢鐤鹃柍鍝勬噹缁愭淇婇妶鍛櫤闁稿顦甸弻銊モ攽閸℃顦遍梺绋款儐閹告悂鍩ユ径濞炬瀻婵☆垵宕甸弳锔戒繆閻愵亜鈧牠寮婚妸鈺佸珘妞ゆ帒鍊婚惌姘舵⒑椤掆偓缁夋挳鎮炲ú顏呯厱闁规澘鑻幊鎰八?
            if (vo.getColumnId() != null) {
                MetadataColumn column = columnMap.get(vo.getColumnId());
                if (column != null) {
                    vo.setColumnName(column.getColumnName());
                }
            }

            // 闂傚倷娴囬褏鑺遍懖鈺佺筏濠电姵鐔紞鏍ь熆閼搁潧濮堥柛搴★攻閵囧嫰寮崒姘闂佺顑嗛敃銏ゅ蓟閺囩喓绠鹃柛顭戝枓閸嬫捇骞橀懜闈涘簥闂佺硶鍓濈粙鎺楁偂?
            if (vo.getCompareTableId() != null) {
                MetadataTable compareTable = tableMap.get(vo.getCompareTableId());
                if (compareTable != null) {
                    vo.setCompareTableName(compareTable.getTableName());
                }
            }

            // 闂傚倷娴囬褏鑺遍懖鈺佺筏濠电姵鐔紞鏍ь熆閼搁潧濮堥柛搴★攻閵囧嫰寮崼鐔告闂佺顑嗛幐鎼佸煡婢跺ň鏋庢俊顖濆吹閺嗭附淇婇悙顏勨偓鏍蓟閵娾晛瀚夋い鎺戝€婚惌姘舵⒑椤掆偓缁夋挳鎮炲ú顏呯厱闁规澘鑻幊鎰八?
            if (vo.getCompareColumnId() != null) {
                MetadataColumn compareColumn = columnMap.get(vo.getCompareColumnId());
                if (compareColumn != null) {
                    vo.setCompareColumnName(compareColumn.getColumnName());
                }
            }
        }
    }

    /**
     * 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顓濇勃闁诡垎鍕偓顓㈡⒒娴ｅ憡鍟炴繛璇х畵瀹曟垿宕熼娑樷偓鍫曟煟閺傚灝鎮戦柣鎾存礃缁绘盯宕卞Ο蹇ｄ邯閹﹢濡烽敂杞扮盎闂佸搫鍟犻崑鎾绘煟濡ゅ啫孝妞?
     */
    private DqcRuleDef requireAccessibleRule(Long id) {
        if (id == null) {
            throw new ServiceException("Rule ID must not be null.");
        }
        DqcRuleDef rule = baseMapper.selectCompatibleById(id);
        if (rule == null) {
            throw new ServiceException("闂傚倷娴囧畷鐢稿窗閹扮増鍋￠柕澹偓閸嬫挸顫濋悡搴♀拫閻庤娲栫紞濠囥€佸☉妯锋婵﹫绲鹃悘鍐煟鎼达紕鐣柛搴ㄤ憾楠炲﹪骞囬弶璺啈闂佺粯顭囩划顖炴偂? " + id);
        }
        // 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顖氱疀妞ゆ挾鍋涙竟鍡樹繆閻愵亜鈧牕顫忔繝姘柧妞ゆ劧绠戦崹鍌氣攽閸屾碍鍟為柣鎾寸☉闇夐柨婵嗘噺閹牊銇勯敐鍫濅汗闁逞屽墲椤骞愭搴ｇ焼濞撴埃鍋撻柛鈺冨仱瀹曟﹢顢欓懖鈺冩澑婵＄偑鍊栧濠氬磻閹剧粯鍋ｅù锝呮贡閸欌偓闂佽鍠涢～澶愬箯閸涙潙宸濆┑鐘插€峰ǎ顔界節?
        if (rule.getTableId() != null) {
            MetadataTable table = metadataTableMapper.selectById(rule.getTableId());
            if (table != null) {
                datasourceHelper.getSysDatasource(table.getDsId());
            }
        }
        return rule;
    }

    /**
     * 闂傚倷娴囧畷鍨叏瀹曞洨鐭嗗ù锝堫潐濞呯姴霉閻樺樊鍎愰柛瀣典邯閺屾盯鍩勯崘澶哥返濠电偞鎹侀崺鏍ㄧ┍婵犲浂鏁嶆繛鎴濆船閸斿矂姊洪崨濠呭婵炲樊鍙冨?
     */
    private void setDefaultValues(DqcRuleDef entity) {
        if (entity.getEnabled() == null) {
            entity.setEnabled("0");
        }
        if (entity.getErrorLevel() == null) {
            entity.setErrorLevel("MEDIUM");
        }
        if (entity.getRuleStrength() == null) {
            entity.setRuleStrength("WEAK");
        }
    }

    // ==================== 闂傚倸鍊烽懗鍫曗€﹂崼銏″床闁圭増婢橀悿顔姐亜閺嶎偄浠滄慨瑙勭叀閺岋綁寮崒姘粯缂備胶濮村鍫曞Φ閸曨垰绫嶉柛灞剧⊕閻濐亪姊洪崨濠勬喛闁稿鎹囧娲嚒閵堝憛銏＄箾鐏炲倸鐏茬€规洖缍婇幖鍦喆閸曨剚顔囬梻浣告贡閾忓酣宕伴弽顓熷剹婵°倕鎳忛悡銉︾節闂堟稒顥犵紒鐘茬－缁辨帡鍩€椤掑嫬绀嬫い鎰Л閸嬫捇骞掗幋顓熷兊濡炪倖鎸炬慨顓炍ｉ崼鐔虹瘈闁冲皝鍋撻柛娑卞帨閵壯呯＜妞ゆ棁顫夊▍濠冦亜閵忥紕鈽夐柍璇叉唉缁犳稒绻濋崘顏佸亾婵傚憡鈷掑ù锝呮贡濠€浠嬫煕閳轰礁顏柟顔瑰墲瀵板嫮浠︾紒銏＄稐?====================

    /**
     * 闂傚倸鍊风粈渚€宕ョ€ｎ喖纾块柟鎯版鎼村﹪鏌ら懝鎵牚濞存粌缍婇弻娑㈠Ψ閵忊剝鐝曢梺鍝ュТ濡繈寮诲☉銏犲嵆闁靛鍎扮花濠氭⒑鐠囪尙绠抽柛鐘崇墵瀵鈽夊鍛澑闂佽宕橀崺鏍倵椤旂晫绠鹃悗鐢殿焾瀛濋梺缁橆殕閹瑰洭鐛崱娑樼妞ゆ棁鍋愰ˇ鏉款渻閵堝棗濮傞柛銊ユ贡缁厼顫濋懜纰樻嫼闁哄鍋炴刊浠嬪礂鐏炵瓔鐔嗙憸搴ㄣ€冮崨瀛樺仼闁割煈鍋呮刊鎾煟閹寸伝顏呯閹烘鈷戦梻鍫熶緱閻掗箖鏌涙繝鍐╁暗闁瑰箍鍨藉畷鍗炩槈濞嗘垵骞?
     */
    public MetadataTable getMetadataTable(Long tableId) {
        if (tableId == null) {
            return null;
        }
        return metadataTableMapper.selectById(tableId);
    }

    /**
     * 闂傚倸鍊风粈渚€宕ョ€ｎ喖纾块柟鎯版鎼村﹪鏌ら懝鎵牚濞存粌缍婇弻娑㈠Ψ閵忊剝鐝曢梺鍝ュТ濡繈寮诲☉銏犲嵆闁靛鍎扮花濠氭⒑鐠囪尙绠抽柛鐘崇墵瀵鈽夊鍛澑闂佽宕橀崺鏍倵椤旂晫绠鹃悗鐢殿焾瀛濋梺缁橆殕閹瑰洭鐛崱娑樼妞ゆ棁鍋愰ˇ鏉款渻閵堝棗濮傞柛銊ユ贡缁厼顫濋懜纰樻嫼闁哄鍋炴刊浠嬪礂鐏炵瓔鐔嗙憸搴ㄣ€冮崨瀛樺仼闁割煈鍋呮刊鎾偠濞戞巻鍋撻崗鍛棜婵犵數鍋為崹鍫曟偡椤栨埃鏋旈柡鍥╁枂娴滄粓鏌￠崒娑橆嚋婵犫偓閹殿喚纾肩€光偓閸曨亝鍠氶梺绯曟櫅鐎氭澘鐣峰Ο娆炬Ь缂備讲鍋撻柍?
     */
    public MetadataColumn getMetadataColumn(Long columnId) {
        if (columnId == null) {
            return null;
        }
        return metadataColumnMapper.selectById(columnId);
    }

    /**
     * 闂傚倸鍊风粈渚€宕ョ€ｎ喖纾块柟鎯版鎼村﹪鏌ら懝鎵牚濞存粌缍婇弻娑㈠Ψ閵忊剝鐝曢梺绋款儐閿曘垽寮婚弴鐔虹鐟滃秶鈧凹鍣ｅ鎶芥偐缂佹ǚ鎷洪梺闈╁瘜閸欏酣寮抽鐐寸厱閻庯綆鍋呭畷宀勬煛瀹€瀣瘈鐎规洜鍘ч埞鎴﹀箛椤撳绠撳娲倻閳轰緡娼氶梺鍛婂姧缁查箖鎮樻笟鈧濠氬磼濮樺崬顤€缂備礁顑嗛崹鍧楀箖濡　鏋庨柟鐐綑娴犫晛鈹戦悙鏉戠伇濞存粠鍓熷畷銏狀煥閸涱垳锛滈柡澶婄墑閸斿酣骞婇崶顒佺厵妞ゆ棁宕甸惌娆忊攽閳╁啯鍊愬┑鈥崇埣瀹曞爼顢旈崟鍨棨闂傚倸鍊风粈渚€骞夐敓鐘茬闁告縿鍎抽惌鎾绘煕椤愶絾澶勯柡浣稿€归妵鍕箳瀹ュ牆鍘￠梺宕囩帛濡啴骞冨Δ鍛櫜閹肩补鈧尙鐩庢俊鐐€曠€涒晠鎮烽埡渚囨綎婵炲樊浜堕弫鍐煏韫囧﹥娅嗛弫鍫ユ⒑鐠囨彃顒㈤柛婵嗛叄瀹曟繈骞嬮敃鈧弸?
     */
    public List<MetadataColumn> getTableColumns(Long tableId) {
        if (tableId == null) {
            return List.of();
        }
        return metadataColumnMapper.selectByTableId(tableId);
    }

    /**
     * 闂傚倸鍊风粈渚€宕ョ€ｎ喖纾块柟鎯版鎼村﹪鏌ら懝鎵牚濞存粌缍婇弻娑㈠Ψ閵忊剝婢掗梺绋款儐閹稿骞忛崨鏉戠闁绘劦鍓涚粔鐑芥⒒娴ｉ涓查悗闈涚焸瀹曟澘顫濈捄铏圭暫婵炴潙鍚嬪娆愬劔闂備線娼ч悧鍡欌偓姘煎枤缁顫濋懜纰樻嫼闁荤姴娲犻埀顒€鍟跨€涳綁姊洪幖鐐插闁哄拋鍋勫嵄闁归偊鍏橀弸搴ㄦ煙鐎电浠滅痪缁㈠灦濮婃椽妫冨☉杈╁姼闂佸憡鏌ㄩ敃顏堝春閳?闂傚倷娴囧畷鍨叏閸偆顩插ù鐘差儏缁€澶屸偓鍏夊亾闁告洦鍓欐禒閬嶆偡濠婂啰效闁糕斂鍨介獮妯尖偓娑櫭鎾绘⒑閸涘﹤濮夐柛瀣尵缁辩偤宕奸妷锔规嫼闂佸憡绋戦敃銈囩箔濮樿埖鐓熼柣鏃囶唺閸氼偅淇?
     */
    public MetadataTable getCompareTable(Long compareTableId) {
        if (compareTableId == null) {
            return null;
        }
        return metadataTableMapper.selectById(compareTableId);
    }

    /**
     * 闂傚倸鍊风粈渚€宕ョ€ｎ喖纾块柟鎯版鎼村﹪鏌ら懝鎵牚濞存粌缍婇弻娑㈠Ψ閵忊剝婢掗梺绋款儐閹稿骞忛崨鏉戠闁绘劦鍓涚粔鐑芥⒒娴ｉ涓查悗闈涚焸瀹曟澘顫濈捄铏圭暫婵炴潙鍚嬪娆愬劔闁荤喐绮岄柊锝夊箖妤ｅ啯鐒肩€广儱妫涢崢鐢电磼閸撗冾暭闁挎艾霉濠婂牏鐣烘慨濠冩そ瀹曨偊宕熼鐑嗘毇婵＄偑鍊х€靛矂宕归崼鏇炵畺濡わ附顑欏Σ濠氭⒑瀹曞洨甯涢柟鐟版搐铻為柛鎰╁妷濡插牊淇婇鐐达紵缂侇喗鎸冲娲嚒閵堝懍娌繛鎾寸椤ㄥ﹪骞冮埄鍐ㄧ窞閻忕偟铏庡鐔封攽閻愬弶顥為柟绋款煼瀹曟垿濡搁埡鍌滃帾婵犵數鍋涢悘婵嬪焵椤掍緡娈滈柟铏箖閵堬綁宕橀埡鍐ㄥ箞闂備浇顫夐崕宕囧椤撯€斥偓瀛樹繆閻愵亜鈧洖霉濮樿泛绀堟慨妯夸含閻瑥顭跨捄鍝勵槵闁稿鎹囬弫鎰償閳ヨ尙鐩庢俊鐐€曠€涒晠鎮烽埡渚囨綎婵炲樊浜堕弫鍐煏韫囧﹥娅嗛弫鍫ユ⒑鐠囨彃顒㈤柛婵嗛叄瀹曟繈骞嬮敃鈧弸?
     */
    public List<MetadataColumn> getCompareTableColumns(Long compareTableId) {
        if (compareTableId == null) {
            return List.of();
        }
        return metadataColumnMapper.selectByTableId(compareTableId);
    }
}



