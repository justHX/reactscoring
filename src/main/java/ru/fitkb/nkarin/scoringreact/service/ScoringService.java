package ru.fitkb.nkarin.scoringreact.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.fitkb.nkarin.scoringreact.Constants;
import ru.fitkb.nkarin.scoringreact.dao.PersonDataDao;
import ru.fitkb.nkarin.scoringreact.dao.entity.PersonData;
import ru.fitkb.nkarin.scoringreact.model.CheckTaxServiceRs;
import ru.fitkb.nkarin.scoringreact.model.PersonDataRq;
import ru.fitkb.nkarin.scoringreact.model.PersonDataRs;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

	private final PersonDataDao personDataDao;

	@Value("${url.tax.service}")
	private String urlCheckTaxService;

	public Mono<PersonDataRs> checkScoring(Mono<Integer> scoring, String inn) {

		PersonDataRs personDataRs = new PersonDataRs();

		if (!checkTaxService(inn)) {

			return scoring.map(personData -> {
				personDataRs.setScores(-1);
				personDataRs.setApprovedCredit(false);
				personDataRs.setCategory("Unreliable client");
				return personDataRs;
			}).cast(PersonDataRs.class);
		}

		return scoring.map(personData -> {
			personDataRs.setScores(scoring.block());

			if (personDataRs.getScores() > 5)
				personDataRs.setApprovedCredit(true);
			else
				personDataRs.setApprovedCredit(false);

			personDataRs.setCategory("Average creditworthiness");
			return personDataRs;
		}).cast(PersonDataRs.class);
	}

	public Mono<Integer> processingPersonData(PersonDataRq personDataRq) {

		Integer scoring = 0;

		if (personDataRq.getPlusMoneyMonth() < personDataRq.getMinusMoneyMonth())
			return Mono.just(-1);

		if (null != personDataRq.getLastName() && !personDataRq.getLastName().isEmpty())
			scoring++;
		if (null != personDataRq.getName() && !personDataRq.getName().isEmpty())
			scoring++;
		if (null != personDataRq.getSecondName() && !personDataRq.getSecondName().isEmpty())
			scoring++;
		if (null != personDataRq.getBirthDate() && !personDataRq.getBirthDate().isEmpty())
			scoring++;
		if (Constants.SEX_MAN.equals(personDataRq.getSex()) || Constants.SEX_WOMAN.equals(personDataRq.getSex()))
			scoring++;
		if (Constants.DOCUMENT_NAME.equals(personDataRq.getDocumentName()))
			scoring++;
		if (null != personDataRq.getSerialAndNumber() && !personDataRq.getSerialAndNumber().isEmpty())
			scoring++;
		if (null != personDataRq.getPhone() && !personDataRq.getPhone().isEmpty())
			scoring++;
		if (null != personDataRq.getAddress() && !personDataRq.getAddress().isEmpty())
			scoring++;

		if (Constants.PEOPLE_OLD > personDataRq.getAge() && Constants.PEOPLE_YANG < personDataRq.getAge())
			scoring++;
		else if (Constants.PEOPLE_YANG == personDataRq.getAge())
			scoring += 3;

		if (Constants.NATIONALITY.equals(personDataRq.getNationality()))
			scoring += 2;
		else
			scoring++;

		if (Constants.MARRIAGE_NO.equals(personDataRq.getMaterialStatus()) && personDataRq.getIsChildren())
			scoring -= 3;
		else if (Constants.MARRIAGE_YES.equals(personDataRq.getMaterialStatus()) && personDataRq.getIsChildren())
			scoring++;
		else if (Constants.MARRIAGE_NO.equals(personDataRq.getMaterialStatus()) && !personDataRq.getIsChildren())
			scoring += 2;

		if (Constants.RICH_PEOPLE >= personDataRq.getPlusMoneyMonth())
			scoring += 5;
		if (Constants.POOR_PEOPLE < personDataRq.getPlusMoneyMonth())
			scoring -= 5;
		else
			scoring++;

		return Mono.just(scoring);
	}

	public void saveToDbPersonData(Mono<PersonDataRs> personDataRs, PersonDataRq personDataRq) {
		PersonData personData = new PersonData();

		personDataRs.subscribe(approvedCredit -> {
			personData.setLastName(personDataRq.getLastName());
			personData.setName(personDataRq.getName());
			personData.setSecondName(personDataRq.getSecondName());
			personData.setAddress(personDataRq.getAddress());
			personData.setAge(personDataRq.getAge());
			personData.setBirthDate(personDataRq.getBirthDate());
			personData.setDocumentName(personDataRq.getDocumentName());
			personData.setSerialAndNumber(personDataRq.getSerialAndNumber());
			personData.setPlusMoneyMonth(personDataRq.getPlusMoneyMonth());
			personData.setMinusMoneyMonth(personDataRq.getMinusMoneyMonth());
			personData.setPhone(personDataRq.getPhone());
			personData.setSex(personDataRq.getSex());
			personData.setApprovedCredit(approvedCredit.getApprovedCredit());
		});

		Mono<PersonData> save = personDataDao.save(personData);

		save.subscribe(s->{
			System.out.println(s.getApprovedCredit());
		});

	}

	public Mono<PersonData> getPersonData(String serialAndNumber) {
		return personDataDao.get(serialAndNumber);
	}

	private boolean checkTaxService(String inn) {

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<CheckTaxServiceRs> response
				= restTemplate.getForEntity(urlCheckTaxService + "/" + inn, CheckTaxServiceRs.class);

		return Objects.requireNonNull(response.getBody()).getApprovedCredit();
	}
}
