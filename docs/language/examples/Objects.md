# Objects, fields, and method calls

## Java source

```java
public class Account {
    private int balance;

    public Account(int openingBalance) {
        balance = openingBalance;
    }

    public int deposit(int amount) {
        balance += amount;
        return balance;
    }

    public static int depositAndRead(int openingBalance, int amount) {
        Account account = new Account(openingBalance);
        account.deposit(amount);
        return account.balance;
    }
}
```

## JVM JASM

```jasm
.version 8
.super java/lang/Object
.class public super Account {
    .field private balance I 

    .method public <init> (I)V {
        parameters: { this, openingBalance },
        code: {
        A: 
            aload this
            invokespecial java/lang/Object.<init> ()V
        B: 
            aload this
            iload openingBalance
            putfield Account.balance I
        C: 
            return 
        D: 
        }
    }

    .method public deposit (I)I {
        parameters: { this, amount },
        code: {
        A: 
            aload this
            dup 
            getfield Account.balance I
            iload amount
            iadd 
            putfield Account.balance I
        B: 
            aload this
            getfield Account.balance I
            ireturn 
        C: 
        }
    }

    .method public static depositAndRead (II)I {
        parameters: { openingBalance, amount },
        code: {
        A: 
            new Account
            dup 
            iload openingBalance
            invokespecial Account.<init> (I)V
            astore account
        B: 
            aload account
            iload amount
            invokevirtual Account.deposit (I)I
            pop 
        C: 
            aload account
            getfield Account.balance I
            ireturn 
        D: 
        }
    }
}
```

## Dalvik JASM

```jasm
.super java/lang/Object
.class public Account {
    .field private balance I 

    .method public constructor <init> (I)V {
        registers: 2,
        parameters: { openingBalance },
        code: {
        A: 
            invoke-direct { v0 } java/lang/Object.<init> ()V
        B: 
            iput v1 v0 Account.balance I
        C: 
            return-void 
        D: 
        }
    }

    .method public static depositAndRead (II)I {
        registers: 4,
        parameters: { openingBalance, amount },
        code: {
        A: 
            new-instance account LAccount;
            invoke-direct { account, v2 } Account.<init> (I)V
        B: 
            invoke-virtual { account, v3 } Account.deposit (I)I
        C: 
            iget v1 account Account.balance I
            return v1
        D: 
        }
    }

    .method public deposit (I)I {
        registers: 3,
        parameters: { amount },
        code: {
        A: 
            iget v0 v1 Account.balance I
            add-int/2addr v0 v2
            iput v0 v1 Account.balance I
        B: 
            iget v0 v1 Account.balance I
            return v0
        C: 
        }
    }
}
```
