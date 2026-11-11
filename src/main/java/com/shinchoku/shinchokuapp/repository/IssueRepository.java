package com.shinchoku.shinchokuapp.repository;

import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface IssueRepository extends WorkspaceScopedRepository<Issue> {}
