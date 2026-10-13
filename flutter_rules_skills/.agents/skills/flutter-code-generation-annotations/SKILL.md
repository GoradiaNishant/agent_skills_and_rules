---
name: flutter-code-generation-annotations
description: >-
  Standardized annotations, code generation practices, and immutability rules for Flutter
  and Dart projects. Covers Freezed, JsonSerializable, Injectable, build_runner, and clean data contracts.
---

# Flutter Code Generation & Annotation Standards Skill

You are an expert Flutter architect and code generation specialist.

## 1. Core Principles

1. **Immutable Domain & State Models**: All UI states, domain entities, and data transfer objects must be immutable. Use `@freezed` to generate value equality, `copyWith`, and pattern matching.
2. **Resilient JSON Deserialization**: Use `@JsonSerializable` with defensive configurations:
   - Always tolerate missing or unknown keys.
   - Use nullable fields with safe defaults for remote payloads.
   - Separate remote DTOs (`UserDto`) from domain models (`User`).
3. **Decoupled Dependency Injection**: Use `@injectable`, `@singleton`, or `@lazySingleton` to register components into `GetIt`. Never instantiate dependencies directly inside Widgets or repositories.
4. **Clean Code Generation Discipline**: Always run `dart run build_runner build --delete-conflicting-outputs`. Never manually edit generated files (`*.freezed.dart`, `*.g.dart`, `*.config.dart`).

---

## 2. Production Patterns

### 1. Freezed State Unions (UI State Modeling)
```dart
import 'package:freezed_annotation/freezed_annotation.dart';

part 'product_state.freezed.dart';

@freezed
sealed class ProductState with _$ProductState {
  const factory ProductState.initial() = _Initial;
  const factory ProductState.loading() = _Loading;
  const factory ProductState.success(List<Product> products) = _Success;
  const factory ProductState.error(UiText message) = _Error;
}
```

### 2. Resilient DTO Serialization
```dart
import 'package:json_annotation/json_annotation.dart';

part 'product_dto.g.dart';

@JsonSerializable(explicitToJson: true, createToJson: false)
class ProductDto {
  final String? id;
  final String? name;
  final double? price;

  const ProductDto({this.id, this.name, this.price});

  factory ProductDto.fromJson(Map<String, dynamic> json) =>
      _$ProductDtoFromJson(json);
}
```

### 3. Injectable DI Declarations
```dart
import 'package:injectable/injectable.dart';

@lazySingleton
class ProductRepositoryImpl implements ProductRepository {
  final DioClient _dioClient;
  ProductRepositoryImpl(this._dioClient);
}
```
