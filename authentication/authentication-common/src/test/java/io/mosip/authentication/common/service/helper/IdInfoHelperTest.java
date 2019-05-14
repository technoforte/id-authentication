package io.mosip.authentication.common.service.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.authentication.common.service.config.IDAMappingConfig;
import io.mosip.authentication.common.service.factory.IDAMappingFactory;
import io.mosip.authentication.common.service.helper.IdInfoHelper;
import io.mosip.authentication.common.service.impl.IdInfoFetcherImpl;
import io.mosip.authentication.common.service.impl.match.DemoAuthType;
import io.mosip.authentication.common.service.impl.match.DemoMatchType;
import io.mosip.authentication.core.exception.IdAuthenticationBusinessException;
import io.mosip.authentication.core.indauth.dto.AuthRequestDTO;
import io.mosip.authentication.core.indauth.dto.IdType;
import io.mosip.authentication.core.indauth.dto.IdentityInfoDTO;
import io.mosip.authentication.core.indauth.dto.LanguageType;
import io.mosip.authentication.core.spi.indauth.match.AuthType;
import io.mosip.authentication.core.spi.indauth.match.MatchInput;
import io.mosip.authentication.core.spi.indauth.match.MatchingStrategyType;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class, IDAMappingFactory.class,
		IDAMappingConfig.class })

@RunWith(SpringRunner.class)

@WebMvcTest
public class IdInfoHelperTest {

	@InjectMocks
	IdInfoHelper idInfoHelper;

	@InjectMocks
	IdInfoFetcherImpl idInfoFetcherImpl;

	@Autowired
	private Environment environment;

	@Autowired
	private IDAMappingConfig idMappingConfig;

	@Before
	public void before() {
		ReflectionTestUtils.setField(idInfoHelper, "idInfoFetcher", idInfoFetcherImpl);
		ReflectionTestUtils.setField(idInfoHelper, "environment", environment);
		ReflectionTestUtils.setField(idInfoHelper, "idMappingConfig", idMappingConfig);
		ReflectionTestUtils.setField(idInfoFetcherImpl, "environment", environment);
	}

	@Test
	public void TestgetLanguageName() {
		String langCode = "ara";
		MockEnvironment mockenv = new MockEnvironment();
		mockenv.merge(((AbstractEnvironment) environment));
		mockenv.setProperty("mosip.phonetic.lang.".concat(langCode.toLowerCase()), "arabic-ar");
		mockenv.setProperty("mosip.phonetic.lang.ar", "arabic-ar");
		ReflectionTestUtils.setField(idInfoFetcherImpl, "environment", mockenv);
		Optional<String> languageName = idInfoFetcherImpl.getLanguageName(langCode);
		String value = languageName.get();
		assertEquals("arabic", value);
	}

	@Test
	public void TestgetLanguageCode() {
		String priLangCode = "mosip.primary-language";
		String secLangCode = "mosip.secondary-language";
		MockEnvironment mockenv = new MockEnvironment();
		mockenv.merge(((AbstractEnvironment) environment));
		mockenv.setProperty(priLangCode, "ara");
		mockenv.setProperty(secLangCode, "fra");
		String languageCode = idInfoFetcherImpl.getLanguageCode(LanguageType.PRIMARY_LANG);
		assertEquals("ara", languageCode);
		String languageCode2 = idInfoFetcherImpl.getLanguageCode(LanguageType.SECONDARY_LANG);
		assertEquals("fra", languageCode2);
	}

	@Test
	public void TestValidgetIdentityValuefromMap() {
		List<IdentityInfoDTO> identityList = getValueList();
		Map<String, List<IdentityInfoDTO>> bioIdentity = new HashMap<>();
		String key = "FINGER_Left IndexFinger_2";
		bioIdentity.put("documents.individualBiometrics", identityList);
		Map<String, Entry<String, List<IdentityInfoDTO>>> map = new HashMap<>();
		map.put("FINGER_Left IndexFinger_2", new SimpleEntry<>("leftIndex", identityList));
		ReflectionTestUtils.invokeMethod(idInfoHelper, "getIdentityValueFromMap", key, "ara", map);
	}

	@Test
	public void TestInvalidtIdentityValuefromMap() {
		String language = "ara";
		String key = "FINGER_Left IndexFinger_2";
		Map<String, Entry<String, List<IdentityInfoDTO>>> map = new HashMap<>();
		List<IdentityInfoDTO> identityList = new ArrayList<>();
		map.put("FINGER_Left IndexFinger_2", new SimpleEntry<>("leftIndex", identityList));
		ReflectionTestUtils.invokeMethod(idInfoHelper, "getIdentityValueFromMap", key, language, map);
	}

	@Test
	public void TestgetIdentityValue() {
		List<IdentityInfoDTO> identityInfoList = getValueList();
		String language = "ara";
		String key = "FINGER_Left IndexFinger_2";
		Map<String, List<IdentityInfoDTO>> demoInfo = new HashMap<>();
		demoInfo.put(key, identityInfoList);
		ReflectionTestUtils.invokeMethod(idInfoFetcherImpl, "getIdentityValue", key, language, demoInfo);
	}

	@Test
	public void TestInvalidIdentityValue() {
		String key = "FINGER_Left IndexFinger_2";
		List<IdentityInfoDTO> identityInfoList = null;
		Map<String, List<IdentityInfoDTO>> demoInfo = new HashMap<>();
		demoInfo.put(key, identityInfoList);
		ReflectionTestUtils.invokeMethod(idInfoFetcherImpl, "getIdentityValue", key, "ara", demoInfo);
	}

	@Test
	public void checkLanguageType() {
		ReflectionTestUtils.invokeMethod(idInfoFetcherImpl, "checkLanguageType", null, null);
	}

	@Test
	public void checkLanguageTypeEmpty() {
		ReflectionTestUtils.invokeMethod(idInfoFetcherImpl, "checkLanguageType", "", "");
	}

	@Test
	public void checkLanguageTypenull() {
		ReflectionTestUtils.invokeMethod(idInfoFetcherImpl, "checkLanguageType", "null", "null");
	}

	@Test
	public void TestgetUinType() {
		AuthRequestDTO authRequestDTO = new AuthRequestDTO();
		authRequestDTO.setIndividualIdType(IdType.UIN.getType());
		IdType uinType = idInfoFetcherImpl.getUinOrVidType(authRequestDTO);
		assertEquals(IdType.UIN, uinType);
	}

	@Test
	public void TestgetVidType() {
		AuthRequestDTO authRequestDTO = new AuthRequestDTO();
		authRequestDTO.setIndividualIdType(IdType.VID.getType());
		IdType uinType = idInfoFetcherImpl.getUinOrVidType(authRequestDTO);
		assertEquals(IdType.VID, uinType);
	}

	@Test
	public void TestgetUinorVid() {
		AuthRequestDTO authRequestDTO = new AuthRequestDTO();
		authRequestDTO.setIndividualId("274390482564");
		Optional<String> uinOrVid = idInfoFetcherImpl.getUinOrVid(authRequestDTO);
		assertNotEquals(Optional.empty(), uinOrVid.get());
	}

	@Test
	public void TestEmail() {
		Map<String, List<IdentityInfoDTO>> idInfo = new HashMap<>();
		List<IdentityInfoDTO> identityInfoList = new ArrayList<>();
		IdentityInfoDTO identityInfoDTO = new IdentityInfoDTO();
		identityInfoDTO.setValue("test@test.com");
		identityInfoList.add(identityInfoDTO);
		idInfo.put("phoneNumber", identityInfoList);
	}

	@Test
	public void TestgetEntityInfoAsString() throws IdAuthenticationBusinessException {
		Map<String, List<IdentityInfoDTO>> idInfo = new HashMap<>();
		List<IdentityInfoDTO> identityInfoList = new ArrayList<>();
		IdentityInfoDTO identityInfoDTO = new IdentityInfoDTO();
		identityInfoDTO.setValue("test@test.com");
		identityInfoList.add(identityInfoDTO);
		idInfo.put("phoneNumber", identityInfoList);
		idInfoHelper.getEntityInfoAsString(DemoMatchType.ADDR, idInfo);
	}

	@Test
	public void TestgetAuthReqestInfo() {
		AuthRequestDTO authRequestDTO = new AuthRequestDTO();
		idInfoHelper.getAuthReqestInfo(DemoMatchType.ADDR, authRequestDTO);
	}

	@SuppressWarnings("null")
	@Test
	public void TestmatchIdentityData() throws IdAuthenticationBusinessException {
		AuthRequestDTO authRequestDTO = new AuthRequestDTO();
		Map<String, List<IdentityInfoDTO>> identityEntity = new HashMap<>();
		List<IdentityInfoDTO> identityInfoList = new ArrayList<>();
		IdentityInfoDTO identityInfoDTO = new IdentityInfoDTO();
		identityInfoDTO.setValue("test@test.com");
		identityInfoList.add(identityInfoDTO);
		identityEntity.put("phoneNumber", identityInfoList);
		List<MatchInput> listMatchInputsExp = new ArrayList<>();
		AuthType demoAuthType = null;
		Map<String, Object> matchProperties = new HashMap<>();
		listMatchInputsExp.add(new MatchInput(demoAuthType, DemoMatchType.PHONE, MatchingStrategyType.PARTIAL.getType(),
				60, matchProperties, "fra"));
		idInfoHelper.matchIdentityData(authRequestDTO, identityEntity, listMatchInputsExp, "12523823232");
	}

	@Test
	public void TestisMatchtypeEnabled() {
		idInfoHelper.isMatchtypeEnabled(DemoMatchType.GENDER);
	}

	private List<IdentityInfoDTO> getValueList() {
		String value = "Rk1SACAyMAAAAAEIAAABPAFiAMUAxQEAAAAoJ4CEAOs8UICiAQGXUIBzANXIV4CmARiXUEC6AObFZIB3ALUSZEBlATPYZICIAKUCZEBmAJ4YZEAnAOvBZIDOAKTjZEBCAUbQQ0ARANu0ZECRAOC4NYBnAPDUXYCtANzIXUBhAQ7bZIBTAQvQZICtASqWZEDSAPnMZICaAUAVZEDNAS63Q0CEAVZiSUDUAT+oNYBhAVprSUAmAJyvZICiAOeyQ0CLANDSPECgAMzXQ0CKAR8OV0DEAN/QZEBNAMy9ZECaAKfwZEC9ATieUEDaAMfWUEDJAUA2NYB5AVttSUBKAI+oZECLAG0FZAAA";
		IdentityInfoDTO identityInfoDTO = new IdentityInfoDTO();
		String language = "ara";
		identityInfoDTO.setLanguage(language);
		identityInfoDTO.setValue(value);
		List<IdentityInfoDTO> identityList = new ArrayList<>();
		identityList.add(identityInfoDTO);
		return identityList;
	}

}
