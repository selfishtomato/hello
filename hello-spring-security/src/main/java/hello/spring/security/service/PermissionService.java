package hello.spring.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import hello.spring.security.repo.PermissionRepository;

@Service
public class PermissionService {

	@Autowired
	private PermissionRepository permissionRepository;

	public PermissionRepository getPermissionRepository() {
		return permissionRepository;
	}
}
