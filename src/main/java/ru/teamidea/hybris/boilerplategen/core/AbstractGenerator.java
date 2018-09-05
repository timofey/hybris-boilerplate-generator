package ru.teamidea.hybris.boilerplategen.core;

import ru.teamidea.hybris.boilerplategen.core.enums.LayerEnum;

import java.util.*;

/**
 * Created by Timofey Klyubin on 04.09.18
 */
public abstract class AbstractGenerator {
    public static final Map<String, Set<LayerEnum>> layersMap;
    static {
        final Map<String, Set<LayerEnum>> tmpMap = new HashMap<>(3, 1.0f);
        tmpMap.put("dao", Collections.singleton(LayerEnum.DAO));

        final Set<LayerEnum> tmpSet1 = new HashSet<>(2, 1.0f);
        tmpSet1.add(LayerEnum.DAO);
        tmpSet1.add(LayerEnum.SERVICE);
        tmpMap.put("service", Collections.unmodifiableSet(tmpSet1));

        final Set<LayerEnum> tmpSet2 = new HashSet<>(3, 1.0f);
        tmpSet2.add(LayerEnum.DAO);
        tmpSet2.add(LayerEnum.SERVICE);
        tmpSet2.add(LayerEnum.FACADE);
        tmpMap.put("facade", Collections.unmodifiableSet(tmpSet2));

        layersMap = Collections.unmodifiableMap(tmpMap);
    }
}
