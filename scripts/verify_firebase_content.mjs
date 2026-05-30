import { existsSync, readFileSync } from "node:fs";

function main() {
  const options = parseArgs(process.argv.slice(2));
  const seedPath = options.seedPath || "scripts/firebase_content_seed.json";
  if (!existsSync(seedPath)) {
    throw new Error(`Seed file not found: ${seedPath}`);
  }

  const seed = JSON.parse(readFileSync(seedPath, "utf8"));
  const findings = [];

  verifyTranslations(seed, findings);
  verifyOnboardingContent(seed, findings);
  verifyPracticeCategories(seed, findings);
  verifyBehaviorConfig(seed, findings);
  verifyExercisePrescriptionRules(seed, findings);
  verifyStarterTemplateCoverage(seed, findings);

  if (findings.length == 0) {
    console.log("Content verification passed.");
    return;
  }

  console.error("Content verification failed:");
  findings.forEach((finding) => console.error(`- ${finding}`));
  process.exit(1);
}

function parseArgs(args) {
  const options = {};
  for (let index = 0; index < args.length; index += 1) {
    const argument = args[index];
    switch (argument) {
      case "--seed":
        options.seedPath = args[++index];
        break;
      case "--help":
        console.log("Usage: node scripts/verify_firebase_content.mjs [--seed path]");
        process.exit(0);
      default:
        throw new Error(`Unknown argument: ${argument}`);
    }
  }
  return options;
}

function verifyTranslations(seed, findings) {
  const appContent = seed.app_content || {};
  ["home", "coach", "onboarding"].forEach((docId) => {
    const translations = appContent[docId]?.translations || {};
    if (!translations.en) findings.push(`app_content/${docId} is missing translations.en`);
    if (!translations.vi) findings.push(`app_content/${docId} is missing translations.vi`);
  });

  const templates = seed.starter_plan_templates || {};
  for (const [templateId, template] of Object.entries(templates)) {
    if (!template.translations?.en) findings.push(`starter_plan_templates/${templateId} is missing translations.en`);
    if (!template.translations?.vi) findings.push(`starter_plan_templates/${templateId} is missing translations.vi`);
  }

  const prescriptions = appContent.exercise_prescriptions?.items || {};
  for (const [itemId, item] of Object.entries(prescriptions)) {
    const translations = item.translations || {};
    if (translations.en && !translations.vi) findings.push(`exercise_prescriptions/${itemId} has en note but missing vi`);
    if (translations.vi && !translations.en) findings.push(`exercise_prescriptions/${itemId} has vi note but missing en`);
  }

  const categories = appContent.practice_categories?.items || {};
  for (const [itemId, item] of Object.entries(categories)) {
    if (!item.labels?.en) findings.push(`practice_categories/${itemId} is missing labels.en`);
    if (!item.labels?.vi) findings.push(`practice_categories/${itemId} is missing labels.vi`);
  }

  const quickWorkout = appContent.behavior?.items?.quick_workout;
  if (!quickWorkout?.translations?.en) findings.push("app_content/behavior/items/quick_workout is missing translations.en");
  if (!quickWorkout?.translations?.vi) findings.push("app_content/behavior/items/quick_workout is missing translations.vi");
}

function verifyOnboardingContent(seed, findings) {
  const onboarding = seed.app_content?.onboarding?.translations || {};
  for (const [language, content] of Object.entries(onboarding)) {
    if (!Array.isArray(content.stepTitles) || content.stepTitles.length === 0) {
      findings.push(`app_content/onboarding translations.${language}.stepTitles must contain at least one title`);
    }
    verifyChoiceList(content.goals, `onboarding.${language}.goals`, findings);
    verifyChoiceList(content.fitnessLevels, `onboarding.${language}.fitnessLevels`, findings);
    verifyChoiceList(content.preferredTimes, `onboarding.${language}.preferredTimes`, findings);
    verifyChoiceList(content.durations, `onboarding.${language}.durations`, findings);
    verifyChoiceList(content.equipments, `onboarding.${language}.equipments`, findings);
    verifyChoiceList(content.nutritionStyles, `onboarding.${language}.nutritionStyles`, findings);
    verifyChoiceList(content.workoutDays, `onboarding.${language}.workoutDays`, findings);
    verifyChoiceList(content.restrictions, `onboarding.${language}.restrictions`, findings);
    verifyChoiceList(content.reminders, `onboarding.${language}.reminders`, findings);
  }
}

function verifyPracticeCategories(seed, findings) {
  const categories = seed.app_content?.practice_categories?.items || {};
  const seenIds = new Set();
  for (const [itemId, item] of Object.entries(categories)) {
    const categoryId = item.id || itemId;
    if (seenIds.has(categoryId)) {
      findings.push(`practice_categories/${itemId} duplicates category id ${categoryId}`);
    }
    seenIds.add(categoryId);
    if (!Array.isArray(item.bodyPartKeys) || item.bodyPartKeys.length === 0) {
      findings.push(`practice_categories/${itemId} must define at least one bodyPartKey`);
    }
    if (!item.assetImage) {
      findings.push(`practice_categories/${itemId} is missing assetImage`);
    }
    if (item.order == null || Number.isNaN(Number(item.order))) {
      findings.push(`practice_categories/${itemId} is missing numeric order`);
    }
  }
}

function verifyBehaviorConfig(seed, findings) {
  const behaviorItems = seed.app_content?.behavior?.items || {};
  const home = behaviorItems.home || {};
  const track = behaviorItems.track || {};
  const quickWorkout = behaviorItems.quick_workout || {};

  if (!isPositiveInteger(home.mealTargetPerDay)) {
    findings.push("app_content/behavior/items/home.mealTargetPerDay must be > 0");
  }
  if (!isPositiveInteger(home.waterTargetMl)) {
    findings.push("app_content/behavior/items/home.waterTargetMl must be > 0");
  }

  if (!isPositiveInteger(track.mealTargetPerDay)) {
    findings.push("app_content/behavior/items/track.mealTargetPerDay must be > 0");
  }
  if (!isPositiveInteger(track.activeMinutesPerWorkout)) {
    findings.push("app_content/behavior/items/track.activeMinutesPerWorkout must be > 0");
  }

  if (!isPositiveInteger(quickWorkout.targetExerciseCount)) {
    findings.push("app_content/behavior/items/quick_workout.targetExerciseCount must be > 0");
  }
  if (!Array.isArray(quickWorkout.preferredBodyPartOrder) || quickWorkout.preferredBodyPartOrder.length === 0) {
    findings.push("app_content/behavior/items/quick_workout.preferredBodyPartOrder must contain at least one body part");
  }
  if (!isPositiveInteger(quickWorkout.defaultDurationSeconds)) {
    findings.push("app_content/behavior/items/quick_workout.defaultDurationSeconds must be > 0");
  }
  if (!isPositiveInteger(quickWorkout.defaultSets)) {
    findings.push("app_content/behavior/items/quick_workout.defaultSets must be > 0");
  }
  const quickWorkoutCalories = quickWorkout.translations?.en?.caloriesPerMinute;
  if (typeof quickWorkoutCalories !== "number" || quickWorkoutCalories <= 0) {
    findings.push("app_content/behavior/items/quick_workout.translations.en.caloriesPerMinute must be > 0");
  }
  const quickWorkoutCaloriesVi = quickWorkout.translations?.vi?.caloriesPerMinute;
  if (typeof quickWorkoutCaloriesVi !== "number" || quickWorkoutCaloriesVi <= 0) {
    findings.push("app_content/behavior/items/quick_workout.translations.vi.caloriesPerMinute must be > 0");
  }
}

function verifyExercisePrescriptionRules(seed, findings) {
  const prescriptions = seed.app_content?.exercise_prescriptions?.items || {};
  for (const [itemId, item] of Object.entries(prescriptions)) {
    const exerciseId = item.exerciseId;
    if (!exerciseId) {
      findings.push(`exercise_prescriptions/${itemId} is missing exerciseId`);
      continue;
    }
    const rules = item.rules || [];
    if (rules.length === 0) {
      findings.push(`exercise_prescriptions/${itemId} has no rules`);
    }
    rules.forEach((rule, index) => {
      if (!rule.sets || rule.sets <= 0) {
        findings.push(`exercise_prescriptions/${itemId} rule[${index}] must define sets > 0`);
      }
      const hasPrescription = !!rule.reps || !!rule.durationSeconds;
      if (!hasPrescription) {
        findings.push(`exercise_prescriptions/${itemId} rule[${index}] must define reps or durationSeconds`);
      }
      if (rule.minWeightKg != null && rule.maxWeightKg != null && rule.minWeightKg > rule.maxWeightKg) {
        findings.push(`exercise_prescriptions/${itemId} rule[${index}] has minWeightKg > maxWeightKg`);
      }
      if (rule.bodyWeightMultiplier != null && rule.bodyWeightMultiplier <= 0) {
        findings.push(`exercise_prescriptions/${itemId} rule[${index}] bodyWeightMultiplier must be > 0`);
      }
      if (rule.fixedTargetWeightKg != null && rule.fixedTargetWeightKg <= 0) {
        findings.push(`exercise_prescriptions/${itemId} rule[${index}] fixedTargetWeightKg must be > 0`);
      }
      if (rule.minSuggestedWeightKg != null && rule.maxSuggestedWeightKg != null && rule.minSuggestedWeightKg > rule.maxSuggestedWeightKg) {
        findings.push(`exercise_prescriptions/${itemId} rule[${index}] has minSuggestedWeightKg > maxSuggestedWeightKg`);
      }
    });

    for (let i = 0; i < rules.length; i += 1) {
      for (let j = i + 1; j < rules.length; j += 1) {
        if (isOverlappingRule(rules[i], rules[j])) {
          findings.push(`exercise_prescriptions/${itemId} rules ${i} and ${j} may overlap`);
        }
      }
    }
  }
}

function verifyStarterTemplateCoverage(seed, findings) {
  const prescriptions = seed.app_content?.exercise_prescriptions?.items || {};
  const coveredExerciseIds = new Set(Object.values(prescriptions).map((item) => item.exerciseId).filter(Boolean));
  const templates = seed.starter_plan_templates || {};

  for (const [templateId, template] of Object.entries(templates)) {
    const exercises = template.scheduledWorkoutTemplates?.exercises || [];
    exercises.forEach((exercise) => {
      if (exercise.exerciseId && !coveredExerciseIds.has(exercise.exerciseId)) {
        findings.push(`starter_plan_templates/${templateId} exercise ${exercise.exerciseId} has no exercise_prescription coverage`);
      }
    });
  }
}

function isOverlappingRule(left, right) {
  const leftGoal = left.goal || "*";
  const rightGoal = right.goal || "*";
  const goalOverlap = leftGoal === "*" || rightGoal === "*" || leftGoal === rightGoal;
  const fitnessOverlap = listOverlap(left.fitnessLevels, right.fitnessLevels);
  const equipmentOverlap = listOverlap(left.equipments, right.equipments);
  const weightOverlap = rangeOverlap(left.minWeightKg, left.maxWeightKg, right.minWeightKg, right.maxWeightKg);
  return goalOverlap && fitnessOverlap && equipmentOverlap && weightOverlap;
}

function verifyChoiceList(list, path, findings) {
  if (!Array.isArray(list) || list.length === 0) {
    findings.push(`${path} must contain at least one option`);
    return;
  }
  const seenValues = new Set();
  list.forEach((entry, index) => {
    if (!entry || typeof entry !== "object") {
      findings.push(`${path}[${index}] must be an object`);
      return;
    }
    if (!entry.value) {
      findings.push(`${path}[${index}] is missing value`);
    }
    if (!entry.label) {
      findings.push(`${path}[${index}] is missing label`);
    }
    if (entry.value && seenValues.has(entry.value)) {
      findings.push(`${path}[${index}] duplicates value ${entry.value}`);
    }
    if (entry.value) {
      seenValues.add(entry.value);
    }
  });
}

function isPositiveInteger(value) {
  return Number.isInteger(value) && value > 0;
}

function listOverlap(left = [], right = []) {
  if (left.length === 0 || right.length === 0) return true;
  return left.some((value) => right.includes(value));
}

function rangeOverlap(leftMin, leftMax, rightMin, rightMax) {
  const aMin = leftMin ?? Number.NEGATIVE_INFINITY;
  const aMax = leftMax ?? Number.POSITIVE_INFINITY;
  const bMin = rightMin ?? Number.NEGATIVE_INFINITY;
  const bMax = rightMax ?? Number.POSITIVE_INFINITY;
  return aMin <= bMax && bMin <= aMax;
}

main();
