---
name: ui-first-mock-api
description: >-
  UI-first development workflow with simulated backend repositories, fake data providers,
  and API contract assumption tracking for Flutter applications. Use when implementing
  screens and business logic before backend APIs are finalized or ready.
---

# Flutter UI-First Mock API Development Skill

You are an expert Flutter developer and system architect.

## 1. Core Principles

1. **Never Block UI on Backend Delivery**: Frontend development must proceed unhindered by backend delays.
2. **Contract First via Interfaces**: Always program against repository and data source interfaces (`abstract class ProductRepository`).
3. **Simulate Realistic Conditions**: Fake repositories must simulate realistic network latency (`await Future.delayed(const Duration(milliseconds: 800))`) and support triggerable failure scenarios (Happy path, Empty list, Error, 401 Session Expired, 500 Server Error).
4. **Transparent Swapping**: Swapping between `FakeProductRepository` and `RealProductRepository` must require zero changes to UI or BLoC/Cubit/Riverpod code, governed entirely by DI (`get_it` / `injectable` environment tags or a configuration flag `const bool useMockData = bool.fromEnvironment('USE_MOCK_DATA')`).
5. **Track Ambiguities in `API_ASSUMPTIONS.md`**: Record every inferred field name, type, and status code in [`templates/API_ASSUMPTIONS.md`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/flutter_rules_skills/.agents/skills/ui-first-mock-api/templates/API_ASSUMPTIONS.md).

---

## 2. Implementation Pattern

```dart
// domain/repository/product_repository.dart
abstract interface class ProductRepository {
  Future<NetworkResult<List<Product>>> getProducts();
}

// data/repository/fake_product_repository.dart
@Environment(Environment.dev)
@Injectable(as: ProductRepository)
class FakeProductRepository implements ProductRepository {
  @override
  Future<NetworkResult<List<Product>>> getProducts() async {
    // Simulate network delay
    await Future.delayed(const Duration(milliseconds: 700));

    // Return realistic typed mock data
    return NetworkResult.success([
      const Product(id: 'prod_1', name: 'Fresh Apples', price: 3.99),
      const Product(id: 'prod_2', name: 'Organic Bananas', price: 1.49),
    ]);
  }
}
```
