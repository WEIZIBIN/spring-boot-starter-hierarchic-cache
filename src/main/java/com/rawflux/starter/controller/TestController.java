package com.rawflux.starter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author weizibin
 * @since 2020/5/17 下午10:51 
 */
@RestController
public class TestController {
	private TestRepository testRepository;

	public TestController(TestRepository testRepository) {
		this.testRepository = testRepository;
	}

	@GetMapping("/cache/get")
	public String get() {
		return testRepository.get();
	}

}
